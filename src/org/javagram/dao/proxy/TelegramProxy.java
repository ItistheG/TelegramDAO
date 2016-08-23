package org.javagram.dao.proxy;

import org.javagram.dao.*;
import org.javagram.dao.Dialog;
import org.javagram.dao.Message;
import org.javagram.dao.proxy.changes.*;
import org.javagram.response.InconsistentDataException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by HerrSergio on 19.05.2016.
 */
public class TelegramProxy extends Observable {
    private TelegramDAO telegramDAO;

    public static final int CONTACT_STATUS_TTL = 30000;
    public static final int PHOTO_TTL = 600000;
    public static final int ASYNC_UPDATE_TTL = 300000;
    public static final int SYNC_UPDATE_TTL = 900000;

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

                me = getMeFromDAO();
                LinkedHashMap<Person, Dialog> list = getList();

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

            int retrieved = 0;

            if (!messages.containsKey(person.getId()) || messages.get(person.getId()).size() == 0) {
                ArrayList<Message> buffer = new ArrayList<>();//telegramDAO.getMessages(person, null, count);
                buffer.add(dialogs.get(person.getId()).getLastMessage());
                messages.put(person.getId(), buffer);
                retrieved += buffer.size();
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
                retrieved += gotten.size();
            }

            if(retrieved > 0) {
                notify(new MessagesRetrieved(dialogs.get(person.getId()),
                        buffer.subList(buffer.size() - retrieved, buffer.size())));
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

    private Date lastUpdate = new Date();

    public UpdateChanges update() {
        return update(USE_SYNC_UPDATE);
    }

    public static final int ALLOW_ASYNC_UPDATE = 0, USE_SYNC_UPDATE = 1, FORCE_SYNC_UPDATE = 2;

    public UpdateChanges update(int updateStyle) {

        try {

            ArrayList<Person> addedPersons = new ArrayList<>();
            LinkedHashMap<Person, Person> changedPersons = new LinkedHashMap<>();
            ArrayList<Person> deletedPersons = new ArrayList<>();

            ArrayList<Dialog> addedDialogs = new ArrayList<>();
            LinkedHashMap<Dialog, Dialog> changedDialogs = new LinkedHashMap<>();
            ArrayList<Dialog> deletedDialogs = new ArrayList<>();

            boolean listChanged = false;

            HashSet<Dialog> dialogsToReset2 = new HashSet<>();

          /*      UpdateChanges updateChanges = getStatusesAndPhotosChanges();
                notify(updateChanges);
                return updateChanges;*/

            Updates asyncUpdates = null;
            try {
                asyncUpdates = telegramDAO.getAsyncUpdates(state, persons, me);
                processStatuses(asyncUpdates.getStatuses());
                processPhotos(asyncUpdates.getUpdatedNames());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(updateStyle == ALLOW_ASYNC_UPDATE && canUseAsync(asyncUpdates)) {

                Collection<Message> messages = asyncUpdates.getMessages().keySet();

                for(Message message : messages) {
                    int buddyIndex = persons.indexOf(message.getBuddy());
                    if(buddyIndex < 0) {
                        continue;
                    }
                    Person buddy = persons.get(buddyIndex);
                    int buddyId = buddy.getId();
                    if(!dialogs.containsKey(buddyId)|| ! this.messages.containsKey(buddyId)) {
                        continue;
                    }
                    this.messages.get(buddyId).add(0, message);
                    if(buddyIndex != 0) {
                        persons.remove(buddyIndex);
                        persons.add(0, buddy);
                    }
                    Dialog dialog = dialogs.get(buddyId).update(message);
                    dialogs.put(buddyId, dialog);
                    dialogsToReset2.add(dialog);
                    listChanged = true;
                }

            } else {

                State beginState = telegramDAO.getState();

                if (updateStyle == FORCE_SYNC_UPDATE || mustUpdate(asyncUpdates) || !state.isTheSameAs(beginState)) {

                    Me newMe;
                    LinkedHashMap<Person, Dialog> list;
                    Updates updates;

                    for (; ; ) {
                        newMe = getMeFromDAO();
                        list = getList();
                        updates = telegramDAO.getUpdates(state);
                        if (beginState.isTheSameAs(updates.getState()))
                            break;
                        else
                            beginState = updates.getState();
                    }

                    processStatuses(updates.getStatuses());
                    processPhotos(updates.getUpdatedNames());

                    ArrayList<Person> personsArrayList = extractPersons(list);
                    HashMap<Integer, Dialog> dialogHashMap = extractDialogs(list);


                    getPersonsUpdates(addedPersons, changedPersons, deletedPersons, personsArrayList);

                    getMeUpdates(changedPersons, newMe);

                    getDialogsUpdates(addedDialogs, changedDialogs, deletedDialogs, dialogHashMap);

                    listChanged = isListChanged(personsArrayList, dialogHashMap);

                    me = newMe;
                    persons = personsArrayList;
                    dialogs = dialogHashMap;

                    HashMap<Integer, ArrayList<Message>> rejectedMessages = rejectSuperfluousMessages();//TODO handle

                    HashSet<Integer> dialogsToReset = resetDialogs(updates);

                    for (Integer personId : dialogsToReset) {
                        if (messages.containsKey(personId))
                            messages.remove(personId);
                        if (dialogs.containsKey(personId))
                            dialogsToReset2.add(dialogs.get(personId));
                    }

                    HashMap<Dialog, ArrayList<Message>> newMessages = acceptNewMessages(updates); //TODO handle
                    dialogsToReset2.addAll(newMessages.keySet());

                    HashMap<Dialog, HashSet<Message>> readMessages = readMessages(updates.getReadMessages()); //TODO handle

                    state = updates.getState();

                }

                lastUpdate = new Date();
            }

            UpdateChanges updateChangesForStatusesAndPhotos = getStatusesAndPhotosChanges();

            UpdateChanges updateChanges = new UpdateChanges(
                    new PersonsChanged(addedPersons, changedPersons, deletedPersons),
                    new DialogsChanged(addedDialogs, changedDialogs, deletedDialogs),
                    listChanged,
                    dialogsToReset2,
                    updateChangesForStatusesAndPhotos.getStatusesChanged(),
                    updateChangesForStatusesAndPhotos.getSmallPhotosChanged(),
                    updateChangesForStatusesAndPhotos.getLargePhotosChanged()
            );

            notify(updateChanges);
            return updateChanges;

        } catch (Exception e) {
            e.printStackTrace();
            throw new UpdateException();
        }
    }

    protected boolean isAsyncBroken(Updates asyncUpdates) {
        return asyncUpdates == null ||
                asyncUpdates.getState() == null ||
                asyncUpdates.isContactListChanged() ||
                asyncUpdates.getUpdatedNames().size() != 0 ||
                asyncUpdates.getDeletedAndRestoredMessages().size() != 0;
    }

    protected boolean canUseAsync(Updates asyncUpdates) {
        return !isAsyncBroken(asyncUpdates) &&
                System.currentTimeMillis() - lastUpdate.getTime() < ASYNC_UPDATE_TTL;
    }

    protected boolean mustUpdate(Updates asyncUpdates) {
        return isAsyncBroken(asyncUpdates) ||
                asyncUpdates.getMessages().size() != 0 ||
                System.currentTimeMillis() - lastUpdate.getTime() >= SYNC_UPDATE_TTL;
    }

    protected void processPhotos(Collection<Integer> updatedPhotos) {
        for(Integer personId : updatedPhotos) {
            if(smallPhotosValidUntil.containsKey(personId)) {
                smallPhotosValidUntil.put(personId, null);
            }
            if(largePhotosValidUntil.containsKey(personId)) {
                largePhotosValidUntil.put(personId, null);
            }
        }
    }

    private void processStatuses(HashMap<Integer, Date> statuses) {
        for(Integer personId : statuses.keySet()) {
            statuses.put(personId, statuses.get(personId));
        }
        statusesValidUntil = new Date(System.currentTimeMillis() + CONTACT_STATUS_TTL);
    }

    private UpdateChanges getStatusesAndPhotosChanges() throws IOException {
        Date now = new Date();

        Set<Person> statusesChanges = getStatusesChanges(now);
        Set<Person> smallPhotosChanges = getPhotoChanges(now, true);
        Set<Person> largePhotosChanges = getPhotoChanges(now, false);

        UpdateChanges updateChanges = new UpdateChanges(
                new PersonsChanged(new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()),
                new DialogsChanged(new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()),
                false,
                new HashSet<>(),
                statusesChanges,
                smallPhotosChanges,
                largePhotosChanges
        );

        return updateChanges;
    }

    private Set<Person> getPhotoChanges(Date now, boolean small) throws IOException {
        HashMap<Integer, Date> photosValidUntil = small ? smallPhotosValidUntil : largePhotosValidUntil;
        Set<Person> photosChanges = new HashSet<>();
        for(Person person : persons) {
            int personId = person.getId();
            if(!photosValidUntil.containsKey(personId))
                continue;
            Date date = photosValidUntil.get(personId);
            if(date == null || date.before(now)) {
                updatePhoto(person, small);
                photosChanges.add(person);
            }
        }
        return photosChanges;
    }

    private Set<Person> getStatusesChanges(Date now) {
        Set<Person> statusesChanges = new HashSet<>();
        if(statusesValidUntil== null || statusesValidUntil.before(now)) {
            updateStatuses();
            statusesChanges = new HashSet<>(persons);
        }
        return statusesChanges;
    }

    private HashMap<Dialog, HashSet<Message>> readMessages(HashSet<Integer> readMessages) {
        HashMap<Dialog, HashSet<Message>> readMessages2 = new HashMap<>();

        for(Integer personId : messages.keySet()) {
            List<Message> messagesForId = messages.get(personId);
            for(int i = 0; i < messagesForId.size(); i++) {
                Message message = messagesForId.get(i);
                if(readMessages.contains(message.getId()) && !message.isRead()) {
                    Message newMessage = message.readIt();
                    messagesForId.set(i, newMessage);
                    readMessages2.putIfAbsent(dialogs.get(personId), new HashSet<>());
                    readMessages2.get(dialogs.get(personId)).add(newMessage);

                }
            }
        }

        for(Dialog dialog : dialogs.values()) {
            if(messages.containsKey(dialog.getBuddy().getId())) {
                int unread = dialog.getUnreadCount();
                List<Message> m = messages.get(dialog.getBuddy().getId());
                for(int i = 0; i < m.size(); i++) {
                    Message message = m.get(i);
                    if(!(message.getSender() instanceof Me) && !message.isRead()) {
                        if(unread > 0)
                            unread--;
                        else {
                            Message newMessage = message.readIt();
                            m.set(i, newMessage);
                            readMessages2.putIfAbsent(dialog, new HashSet<>());
                            readMessages2.get(dialog).add(newMessage);

                        }
                    }
                }
            }
        }
        return readMessages2;
    }

    private HashSet<Integer> resetDialogs(Updates updates) {
        HashSet<Integer> dialogsToReset = new HashSet<>();
        HashSet<Integer> ids = new HashSet<>();
        ids.addAll(updates.getDeletedAndRestoredMessages().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        for(Integer id : ids) {
            for(Integer dialogId : messages.keySet()) {
                List<Message> list = messages.get(dialogId);
                for(Message message : list) {
                    if(id == message.getId()) {
                        dialogsToReset.add(dialogId);
                        break;
                    }
                }
            }
        }
        return dialogsToReset;
    }

    private Me getMeFromDAO() throws IOException {
        return telegramDAO.getMe();
    }

    public Me getMe() {
        return me;
    }

    private LinkedHashMap<Person, Dialog> getList() throws IOException {
        return telegramDAO.getList(false, false);
    }

    private HashMap<Dialog, ArrayList<Message>> acceptNewMessages(Updates updates) {
        HashMap<Dialog, ArrayList<Message>> newMessages = new HashMap<>();

        for(Message message : updates.getMessages().keySet()) {

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
            newMessages.get(dialog).add(0, message);
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

        if(me.equals(person))
            return new Date(System.currentTimeMillis() + CONTACT_STATUS_TTL);

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
            notify(getUpdateChangesForStatuses(persons));
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

        if (!persons.contains(person) && !me.equals(person))
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

        BufferedImage img = updatePhoto(person, small);
        notify(getUpdateChangesForPhotos(person, small));
        return img;
    }

    public void sendMessage(Person person, String text, long randomId) throws IOException {
        if(this.persons.contains(person))
            telegramDAO.sendMessage(person, text, randomId);
        else
            throw new IllegalArgumentException();
    }

    public long sendMessage(Person person, String text) throws IOException {
        if(this.persons.contains(person))
            return telegramDAO.sendMessage(person, text);
        else
            throw new IllegalArgumentException();
    }

    public void readMessages(Message lastMessage) throws IOException {
        if(this.persons.contains(lastMessage.getBuddy()))
            telegramDAO.readMessages(lastMessage);
        else
            throw new IllegalArgumentException();
    }

    public void receivedMessages(Message lastMessage) throws IOException {
        if(this.persons.contains(lastMessage.getBuddy()))
            telegramDAO.receivedMessages(lastMessage);
        else
            throw new IllegalArgumentException();
    }

    private BufferedImage updatePhoto(Person person, boolean small) throws IOException {
        int personId = person.getId();
        BufferedImage[] bufferedImages = telegramDAO.getPhotos(person, small, !small);
        if(!small)
            bufferedImages[0] = bufferedImages[1];
        if(small) {
            smallPhotos.put(personId, bufferedImages[0]);
            smallPhotosValidUntil.put(personId, new Date(System.currentTimeMillis() + PHOTO_TTL));
        } else {
            largePhotos.put(personId, bufferedImages[0]);
            largePhotosValidUntil.put(personId, new Date(System.currentTimeMillis() + PHOTO_TTL));
        }
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

    private static UpdateChanges getUpdateChangesForStatuses(Collection<? extends Person> persons) {
        return new UpdateChanges(
                new PersonsChanged(new ArrayList<Person>(), new LinkedHashMap<Person, Person>(), new ArrayList<Person>()),
                new DialogsChanged(new ArrayList<Dialog>(), new LinkedHashMap<Dialog, Dialog>(), new ArrayList<Dialog>()),
                false,
                new HashSet<Dialog>(),
                new HashSet<Person>(persons),
                new HashSet<Person>(),
                new HashSet<Person>()
        );
    }

    private static UpdateChanges getUpdateChangesForPhotos(Person person, boolean small) {
        if(small) {
            return new UpdateChanges(
                    new PersonsChanged(new ArrayList<Person>(), new LinkedHashMap<Person, Person>(), new ArrayList<Person>()),
                    new DialogsChanged(new ArrayList<Dialog>(), new LinkedHashMap<Dialog, Dialog>(), new ArrayList<Dialog>()),
                    false,
                    new HashSet<Dialog>(),
                    new HashSet<Person>(),
                    new HashSet<Person>(Arrays.asList(person)),
                    new HashSet<Person>()
            );
        } else {
            return new UpdateChanges(
                    new PersonsChanged(new ArrayList<Person>(), new LinkedHashMap<Person, Person>(), new ArrayList<Person>()),
                    new DialogsChanged(new ArrayList<Dialog>(), new LinkedHashMap<Dialog, Dialog>(), new ArrayList<Dialog>()),
                    false,
                    new HashSet<Dialog>(),
                    new HashSet<Person>(),
                    new HashSet<Person>(),
                    new HashSet<Person>(Arrays.asList(person))
            );
        }

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

    public boolean importContact(String phone, String firstName, String lastName) {
        return telegramDAO.importContact(phone, firstName, lastName);
    }

    public boolean deleteContact(Contact contact) {
        return telegramDAO.deleteContact(contact);
    }

    public boolean deleteContact(int contactId) {
        return telegramDAO.deleteContact(contactId);
    }

}
