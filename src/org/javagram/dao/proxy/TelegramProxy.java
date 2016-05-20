package org.javagram.dao.proxy;

import org.javagram.dao.*;
import org.javagram.dao.proxy.changes.*;
import org.javagram.response.InconsistentDataException;

import java.io.IOException;
import java.util.*;

/**
 * Created by HerrSergio on 19.05.2016.
 */
public class TelegramProxy extends Observable {
    private TelegramDAO telegramDAO;

    private ArrayList<Person> persons;
    private HashMap<Integer, Dialog> dialogs;
    private HashMap<Integer, ArrayList<Message>> messages;
    private State state;
    private Me me;

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

    public void update() {
        try {

            State beginState = telegramDAO.getState();

            if(state.isTheSameAs(beginState))
                return;

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


            if(!equals(newMe,me)) {
                me = newMe;
                changedPersons.put(me, newMe);
            }

            ArrayList<Dialog> addedDialogs = new ArrayList<>();
            LinkedHashMap<Dialog, Dialog> changedDialogs = new LinkedHashMap<>();
            ArrayList<Dialog> deletedDialogs = new ArrayList<>();

            for (Integer id : dialogs.keySet()) {
                Dialog d = dialogs.get(id);
                if (!dialogHashMap.containsKey(id)) {
                    deletedDialogs.add(d);
                } else {
                    Dialog d2 = dialogHashMap.get(id);
                    if(!equals(d, d2)) {
                        changedDialogs.put(d, d2);
                    }
                }
            }

            for (Integer id  : dialogHashMap.keySet()) {
                if (!dialogs.containsKey(id)) {
                    addedDialogs.add(dialogHashMap.get(id));
                }
            }

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


            persons = personsArrayList;
            dialogs = dialogHashMap;

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
                add(newMessages, buddy.getId(), message);
            }


            for(Integer personId : new HashSet<>(messages.keySet())) {
                if(!dialogHashMap.containsKey(personId)) {
                    messages.remove(personId);
                    //toNotifyDialogs.add(person);//FIXME null???
                }
            }

            state = updates.getState();

            notify(
                    new UpdateChanges(
                            new PersonsChanged(addedPersons, changedPersons, deletedPersons),
                            new DialogsChanged(addedDialogs, changedDialogs, deletedDialogs),
                            listChanged,
                            newMessages
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private boolean equals(Person p1, Person p2) {
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
            return false;
        }

        return true;
    }

    private boolean equals(Dialog d1, Dialog d2) {
        return d1 == d2 || d1 != null && d2 != null &&
                equals(d1.getBuddy(), d2.getBuddy()) &&
                d1.getUnreadCount() == d2.getUnreadCount() &&
                d1.getLastMessage().getId() == d2.getLastMessage().getId();
    }

    private void add(HashMap<Dialog, ArrayList<Message>> list, int id, Message message) {
        Dialog dialog = dialogs.get(id);
        list.putIfAbsent(dialog, new ArrayList<>());
        list.get(dialog).add(0, message);
    }

}
