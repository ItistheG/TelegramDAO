package org.javagram.dao.proxy;

import org.javagram.dao.*;
import org.javagram.dao.Dialog;
import org.javagram.dao.proxy.changes.*;
import org.javagram.response.InconsistentDataException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by HerrSergio on 19.05.2016.
 */
public class TelegramProxy extends Observable {
    private TelegramDAO telegramDAO;

    public static final int CONTACT_STATUS_TTL = 30000;
    public static final int PHOTO_TTL = 60000;

    private ArrayList<Person> persons;
    private HashMap<Integer, Dialog> dialogs;
    private HashMap<Integer, ArrayList<Message>> messages;
    private State state;
    private Me me;

    private Date statusesValidUntil;
    private HashMap<Integer, Date> statuses;

    private HashMap<Integer, Date> smallPhotosValidUntil;
    private HashMap<Integer, Date> largePhotosValidUntil;
    private HashMap<Integer, BufferedImage> smallPhotos;
    private HashMap<Integer, BufferedImage> largePhotos;

    public TelegramProxy(TelegramDAO telegramDAO) {
        this.telegramDAO = telegramDAO;
        initialize();
    }

    private void initialize() {

        try {
            state = telegramDAO.getState();

            for(;;) {

                me = telegramDAO.getMe();
                LinkedHashMap<Person, Dialog> list = telegramDAO.getList(false, false);

                State endState = telegramDAO.getState();

                persons = extractPersons(list);
                dialogs = extractDialogs(list);

                messages = new HashMap<>();

                updateStatuses();
                updatePhotos();

                if(state.isTheSameAs(endState))
                    break;
                else
                    state = endState;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<Integer, Dialog> extractDialogs(LinkedHashMap<Person, Dialog> list) {
        HashMap<Integer, Dialog> dialogs = new HashMap<>();
        for(Map.Entry<Person, Dialog> entry : list.entrySet()) {
            if(entry.getValue() != null) {
                dialogs.put(entry.getKey().getId(), entry.getValue());
            }
        }
        return dialogs;
    }

    private ArrayList<Person> extractPersons(LinkedHashMap<Person, Dialog> list) {
        return new ArrayList<>(list.keySet());
    }

    public List<Person> getPersons() {
        return Collections.unmodifiableList(persons);
    }

    public Dialog getDialog(Person person) {
        return dialogs.get(person.getId());
    }

    public Map<Person, Dialog> getDialogs(boolean includeEmpty) {
        LinkedHashMap<Person, Dialog> d = new LinkedHashMap<>();
        for(Person person : persons) {
            if(dialogs.containsKey(person.getId()))
                d.put(person, dialogs.get(person.getId()));
            else if(includeEmpty)
                d.put(person, null);
        }
        return Collections.unmodifiableMap(d);
    }

    public List<Message> getMessages(Person person, int count) {

        if (count < 0)
            count = 0;

        if(!persons.contains(person) || !dialogs.containsKey(person.getId()))
            return Collections.EMPTY_LIST;

        try {

            int downloaded = 0;

            if (!messages.containsKey(person.getId())) {
                ArrayList<Message> buffer = telegramDAO.getMessages(person, null, count);
                messages.put(person.getId(), buffer);
                downloaded += buffer.size();
            }

            ArrayList<Message> buffer = messages.get(person.getId());

            while(buffer.size() < count) {
                Message lastMessage = null;
                if(buffer.size() > 0)
                    lastMessage = buffer.get(buffer.size() - 1);
                ArrayList<Message> gotten = telegramDAO.getMessages(person, lastMessage, count - buffer.size());
                if(gotten.size() == 0)
                    break;
                buffer.addAll(gotten);
                downloaded += gotten.size();
            }

            if(downloaded > 0) {
                notify(new MessagesDownloaded(dialogs.get(person.getId()),
                        buffer.subList(buffer.size() - downloaded, buffer.size())));
            }

            count = Math.min(count, buffer.size());


            return Collections.unmodifiableList(buffer.subList(0, count));

        } catch (Exception e) {
            return null;
        }
    }

    public int getAvailableMessagesCount(Person person) {
        if(!persons.contains(person)||!dialogs.containsKey(person.getId()))
            return -1;
        else if(messages.containsKey(person.getId()))
            return messages.get(person.getId()).size();
        else
            return 0;
    }

    public void squeezeMessages(Person person, int count) {
        if(count < 0)
            count = 0;
        if(!persons.contains(person)||!dialogs.containsKey(person.getId()))
            return;
        if(messages.containsKey(person.getId())) {
            ArrayList<Message> buffer = messages.get(person.getId());
            if(buffer.size() > count) {
                while (buffer.size() > count) {
                    buffer.remove(buffer.size() - 1);
                }
                notify(new MessagesSqueezed(dialogs.get(person.getId())));
            }
        }
    }

    public UpdateChanges update() {
        try {

            State beginState = telegramDAO.getState();

            if(state.isTheSameAs(beginState))
                return null;

            Me newMe;
            LinkedHashMap<Person, Dialog> list;
            Updates updates;

            for(;;) {
                newMe = telegramDAO.getMe();
                list = telegramDAO.getList(false, false);
                updates = telegramDAO.getUpdates(state);
                if (beginState.isTheSameAs(updates.getState()))
                    break;
                else
                    beginState = updates.getState();
            }


            ArrayList<Person> personsArrayList = extractPersons(list);
            HashMap<Integer, Dialog> dialogHashMap = extractDialogs(list);


            ArrayList<Person> addedPersons = new ArrayList<>();
            LinkedHashMap<Person, Person> changedPersons = new LinkedHashMap<>();
            ArrayList<Person> deletedPersons = new ArrayList<>();
            getPersonsUpdates(addedPersons, changedPersons, deletedPersons, personsArrayList);

            getMeUpdates(changedPersons, newMe);

            ArrayList<Dialog> addedDialogs = new ArrayList<>();
            LinkedHashMap<Dialog, Dialog> changedDialogs = new LinkedHashMap<>();
            ArrayList<Dialog> deletedDialogs = new ArrayList<>();
            getDialogsUpdates(addedDialogs, changedDialogs, deletedDialogs, dialogHashMap);

            boolean listChanged = isListChanged(personsArrayList, dialogHashMap);

            persons = personsArrayList;
            dialogs = dialogHashMap;

            HashMap<Dialog, ArrayList<Message>> newMessages = acceptNewMessages(updates);
            HashMap<Integer, ArrayList<Message>> rejectedMessages = rejectSuperfluousMessages();//TODO handle

            state = updates.getState();

            UpdateChanges updateChanges = new UpdateChanges(
                            new PersonsChanged(addedPersons, changedPersons, deletedPersons),
                            new DialogsChanged(addedDialogs, changedDialogs, deletedDialogs),
                            listChanged,
                            newMessages/*,
                            rejectedMessages*/
            );

            notify(updateChanges);
            return updateChanges;

        } catch (Exception e) {
            e.printStackTrace();
            throw new UpdateException();
        }
    }

    private HashMap<Dialog, ArrayList<Message>> acceptNewMessages(Updates updates) {
        HashMap<Dialog, ArrayList<Message>> newMessages = new HashMap<>();

        for(Message message : updates.getMessages()) {

            Person buddy = null;

            if(message.getReceiver() instanceof Me)
                buddy = message.getSender();
            else if(message.getSender() instanceof Me)
                buddy = message.getReceiver();
            else
                throw new InconsistentDataException();

            if(!dialogs.containsKey(buddy.getId()))
                continue;

            messages.putIfAbsent(buddy.getId(), new ArrayList<>());
            messages.get(buddy.getId()).add(0, message);

            Dialog dialog = dialogs.get(buddy.getId());
            newMessages.putIfAbsent(dialog, new ArrayList<>());
            newMessages.get(dialog).add(message);
        }
        return newMessages;
    }

    private HashMap<Integer, ArrayList<Message>> rejectSuperfluousMessages() {
        HashMap<Integer, ArrayList<Message>> rejectedMessages = new HashMap<>();
        for(Integer personId : new HashSet<>(messages.keySet())) {
            if(!dialogs.containsKey(personId)) {
                rejectedMessages.put(personId, messages.remove(personId));
            }
        }
        return rejectedMessages;
    }

    private void getPersonsUpdates(List<? super Person> addedPersons, Map<?super Person, ? super Person> changedPersons,
                                   List<? super Person> deletedPersons, List<? extends Person> personsArrayList) {

        for (Person p : persons) {
            if (!personsArrayList.contains(p)) {
                deletedPersons.add(p);
            } else {
                Person p2 = personsArrayList.get(personsArrayList.indexOf(p));
                if(!equals(p, p2)) {
                    changedPersons.put(p, p2);
                }
            }
        }

        for (Person p : personsArrayList) {
            if (!persons.contains(p)) {
                addedPersons.add(p);
            }
        }
    }


    private void getMeUpdates(Map<?super Person, ? super Person> changedPersons, Me newMe) {
        if(!equals(newMe,me)) {
            me = newMe;
            changedPersons.put(me, newMe);
        }
    }

    private void getDialogsUpdates(List<? super Dialog> addedDialogs, Map<? super Dialog, ? super Dialog> changedDialogs,
                                   List<? super Dialog> deletedDialogs, HashMap<Integer, ? extends Dialog> dialogHashMap) {

        for (Integer id : dialogs.keySet()) {
            Dialog d = dialogs.get(id);
            if (!dialogHashMap.containsKey(id)) {
                deletedDialogs.add(d);
            } else {
                Dialog d2 = dialogHashMap.get(id);
                if (!equals(d, d2)) {
                    changedDialogs.put(d, d2);
                }
            }
        }

        for (Integer id : dialogHashMap.keySet()) {
            if (!dialogs.containsKey(id)) {
                addedDialogs.add(dialogHashMap.get(id));
            }
        }
    }

    private boolean isListChanged(ArrayList<Person> personsArrayList, HashMap<Integer, Dialog> dialogHashMap) {
        boolean listChanged = false;

        if(personsArrayList.size() == persons.size()) {
            for (int i = 0; i < persons.size(); i++) {
                Person p1 = persons.get(i);
                Person p2 = personsArrayList.get(i);
                if (!equals(p1, p2)) {
                    listChanged = true;
                    break;
                }
                Dialog d1 = dialogs.get(p1.getId());
                Dialog d2 = dialogHashMap.get(p2.getId());
                if (!equals(d1, d2)) {
                    listChanged = true;
                    break;
                }
            }
        } else {
            listChanged = true;
        }
        return listChanged;
    }

    public void updateAll(TelegramDAO telegramDAO){
        this.telegramDAO = telegramDAO;
        initialize();
        notify(null);
    }

    public void updateAll() {
        updateAll(this.telegramDAO);
    }

    private void notify(Object data) {
        try {
            setChanged();
            notifyObservers(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Date onlineUntil(Person person) {

        if(!persons.contains(person)) {
            throw new IllegalArgumentException();
        }

        int personId = person.getId();
        Date now = new Date();

        if(statuses.containsKey(personId)) {
            Date status = statuses.get(personId);
            if(status.after(now)) {
                return new Date(status.getTime());
            }
        }

        if(statusesValidUntil.before(now)) {
            updateStatuses();
        }

        if(statuses.containsKey(personId)) {
            return new Date(statuses.get(personId).getTime());
        }

        return null;//new Date(0);
    }

    public boolean isOnline(Person person) {
        Date status = onlineUntil(person);
        return (status != null && status.getTime() > System.currentTimeMillis());
    }

    public BufferedImage getPhoto(Person person, boolean small) throws IOException {

        if(!persons.contains(person))
            throw new IllegalArgumentException();

        HashMap<Integer, Date> photosValidUntil = small ? smallPhotosValidUntil : largePhotosValidUntil;
        HashMap<Integer, BufferedImage> photos = small ? smallPhotos : largePhotos;

        int personId = person.getId();

        {
            Date photoValidUntil = photosValidUntil.get(personId);
            if (photoValidUntil != null && photoValidUntil.after(new Date())) {
                //BufferedImage photo = photos.get(personId);
                if (photos.containsKey(personId)) {
                    return photos.get(personId);
                }
            }
        }

        BufferedImage[] bufferedImages = telegramDAO.getPhotos(person, small, !small);
        if(!small)
            bufferedImages[0] = bufferedImages[1];
        photos.put(personId, bufferedImages[0]);
        photosValidUntil.put(personId, new Date(System.currentTimeMillis() + PHOTO_TTL));
        return bufferedImages[0];
    }

    private void updateStatuses() {

        try {
            statuses = new HashMap<>(telegramDAO.getStatuses(persons));
        } catch (Exception e) {
            statuses = new HashMap<>();
        }

        statusesValidUntil = new Date(System.currentTimeMillis() + CONTACT_STATUS_TTL);
    }

    private void updatePhotos() {
        smallPhotos = new HashMap<>();
        largePhotos = new HashMap<>();
        smallPhotosValidUntil = new HashMap<>();
        largePhotosValidUntil = new HashMap<>();
    }

    public static boolean equals(Person p1, Person p2) {
        if(p1.getClass() != p2.getClass())
            return false;
        if(p1.getId() != p2.getId())
            return false;
        if(!p2.getLastName().equals(p1.getLastName()))
            return false;
        if(!p2.getFirstName().equals(p1.getFirstName()))
            return false;

        if(p1 instanceof Foreign) {
            if(((Foreign) p1).getAccessHash() != ((Foreign) p2).getAccessHash())
                return false;
        } else if(p2 instanceof KnownPerson) {
            if(!((KnownPerson) p1).getPhoneNumber().equals(((KnownPerson) p2).getPhoneNumber()))
                return false;
        } else {
            throw new UpdateException(new IllegalArgumentException());
        }

        return true;
    }

    public static boolean equals(Dialog d1, Dialog d2) {
        return d1 == d2 || d1 != null && d2 != null &&
                equals(d1.getBuddy(), d2.getBuddy()) &&
                d1.getUnreadCount() == d2.getUnreadCount() &&
                d1.getLastMessage().getId() == d2.getLastMessage().getId();
    }

}
