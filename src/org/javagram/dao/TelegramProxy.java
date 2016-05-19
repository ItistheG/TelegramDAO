package org.javagram.dao;

import org.javagram.response.InconsistentDataException;

import java.io.IOException;
import java.util.*;

/**
 * Created by HerrSergio on 19.05.2016.
 */
public class TelegramProxy {
    private TelegramDAO telegramDAO;

    private ArrayList<Person> persons;
    private HashMap<Person, Dialog> dialogs;
    private HashMap<Person, ArrayList<Message>> messages;
    private State state;
    private Me me;

    private Observable observable = new Observable() {
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }

        @Override
        public void notifyObservers(Object o) {
            setChanged();
            super.notifyObservers(o);
        }
    };

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

                persons = extractPersons(list);
                dialogs = extractDialogs(list);

                messages = new HashMap<>();

                State endState = telegramDAO.getState();
                if(state.isTheSameAs(endState))
                    break;
                else
                    state = endState;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<Person, Dialog> extractDialogs(LinkedHashMap<Person, Dialog> list) {
        HashMap<Person, Dialog> dialogs = new HashMap<>();
        for(Map.Entry<Person, Dialog> entry : list.entrySet()) {
            if(entry.getValue() != null) {
                dialogs.put(entry.getKey(), entry.getValue());
            }
        }
        return dialogs;
    }

    private ArrayList<Person> extractPersons(LinkedHashMap<Person, Dialog> list) {
        return new ArrayList<>(list.keySet());
    }

    public List<Person> getPersons(/*boolean excludeWithoutDialogs*/) {
        return Collections.unmodifiableList(persons);
    }

    public Dialog getDialog(Person person) {
        return dialogs.get(person);
    }

    public List<Dialog> getDialogs() {
        ArrayList<Dialog> d = new ArrayList<>();
        for(Person person : persons) {
            if(dialogs.containsKey(person))
                d.add(dialogs.get(person));
        }
        return Collections.unmodifiableList(d);
    }

    public List<Message> getMessages(Person person, int count) {

        if (count < 0)
            count = 0;

        if(!persons.contains(person)||!dialogs.containsKey(person))
            return Collections.EMPTY_LIST;

        boolean notify = false;

        try {

            if (!messages.containsKey(person)) {
                ArrayList<Message> buffer = telegramDAO.getMessages(person, null, count);
                notify = true;
                messages.put(person, buffer);
            }

            ArrayList<Message> buffer = messages.get(person);

            while(buffer.size() < count) {
                Message lastMessage = null;
                if(buffer.size() > 0)
                    lastMessage = buffer.get(buffer.size() - 1);
                ArrayList<Message> gotten = telegramDAO.getMessages(person, lastMessage, count - buffer.size());
                if(gotten.size() == 0)
                    break;
                buffer.addAll(gotten);
                notify = true;
            }

            if(notify) {
                notify(dialogs.get(person));
            }

            count = Math.min(count, buffer.size());


            return Collections.unmodifiableList(buffer.subList(0, count));

        } catch (Exception e) {
            return null;
        }
    }

    public int getAvailableMessagesCount(Person person) {
        if(!persons.contains(person)||!dialogs.containsKey(person))
            return -1;
        else if(messages.containsKey(person))
            return messages.get(person).size();
        else
            return 0;
    }

    public void squeezeMessages(Person person, int count) {
        if(count < 0)
            count = 0;
        if(!persons.contains(person)||!dialogs.containsKey(person))
            return;
        if(messages.containsKey(person)) {
            ArrayList<Message> buffer = messages.get(person);
            if(buffer.size() > count) {
                while (buffer.size() > count) {
                    buffer.remove(buffer.size() - 1);
                }
                notify(dialogs.get(person));
            }
        }
    }

    public void update() {
        try {

            if(state.isTheSameAs(telegramDAO.getState()))
                return;

            Me newMe = telegramDAO.getMe();
            LinkedHashMap<Person, Dialog> list = telegramDAO.getList(false, false);
            ArrayList<Person> personsArrayList = extractPersons(list);
            HashMap<Person, Dialog> dialogHashMap = extractDialogs(list);

            Updates updates = telegramDAO.getUpdates(state);

            HashSet<Person> toNotify = new LinkedHashSet<>();
            HashSet<Person> toNotifyDialogs = new LinkedHashSet<>();

            for(Message message : updates.getMessages()) {

                Person buddy = null;

                if(message.getReceiver() instanceof Me)
                    buddy = message.getSender();
                else if(message.getSender() instanceof Me)
                    buddy = message.getReceiver();
                else
                    throw new InconsistentDataException();

                if(messages.containsKey(buddy)) {
                    messages.get(buddy).add(0, message);
                    toNotifyDialogs.add(buddy);
                } else {
                    messages.put(buddy, new ArrayList<>(Arrays.asList(message)));
                    toNotifyDialogs.add(buddy);
                }
            }


            //TODO check fields
            for (Person p : persons) {
                if (!personsArrayList.contains(p)) {
                    toNotify.add(p);
                } else {
                    Person p2 = personsArrayList.get(personsArrayList.indexOf(p));
                    if(!equals(p, p2)) {
                        toNotify.add(p2);
                    }
                }
            }

            for (Person p : personsArrayList) {
                if (!persons.contains(p)) {
                    toNotify.add(p);
                }
            }


            for(Person person : new HashSet<>(messages.keySet())) {
                if(!dialogHashMap.containsKey(person)) {
                    messages.remove(person);
                    toNotifyDialogs.add(person);//FIXME null???
                }
            }

            if(!equals(newMe,me)) {
                me = newMe;
                toNotify.add(newMe);
            }

            //TODO  check contents?
          /*  for (Dialog d : dialogs.values()) {
                Dialog d2 = dialogHashMap.get(d.getBuddy());
                if (d2 == null){
                    toNotify.add(d.getBuddy());
                    continue;
                }

                if(d.getLastMessage().getId() != d2.getLastMessage().getId()
                        || d.getUnreadCount() != d2.)
            }

            for (Person p : personsArrayList) {
                if (!persons.contains(p)) {
                    toNotify.add(p);
                }
            }*/

            boolean needUpdateContactList = false;

            if(personsArrayList.size() == persons.size()) {
                for(int i = 0; i < persons.size(); i++) {
                    Person p1 = persons.get(i);
                    Person p2 = personsArrayList.get(i);
                    if(!equals(p1, p2)) {
                        needUpdateContactList = true;
                        break;
                    }
                    Dialog d1 = dialogs.get(p1);
                    Dialog d2 = dialogHashMap.get(p2);
                    if(!equals(d1, d2)) {
                        needUpdateContactList = true;
                        break;
                    }
                }
            } else {
                needUpdateContactList = true;
            }

            persons = personsArrayList;
            dialogs = dialogHashMap;


            /*boolean equal = true;
            if(personsArrayList.size() == persons.size()) {
                for (int i = 0; i < persons.size(); i++) {
                    Person d1 = persons.get(i);
                    Person d2 = personsArrayList.get(i);

                    equal &= d1.getId() == d2.getId();
                    equal &= d1.getFirstName().equals(d2.getFirstName());
                    equal &= d1.getLastName().equals(d2.getLastName());
                    if(!equal)
                        break;
                }

                if(dialogHashMap.size() == dialogs.size())

            } else {
                equal = false;
            }

            if(!equal) {
                dialogs = new HashMap<>();
                for(Map.Entry<Person, Dialog> entry : list.entrySet()) {
                    if(entry.getValue() != null) {
                        dialogs.put(entry.getKey(), entry.getValue());
                    }
                }
            }*/

            state = updates.getState();

            if(needUpdateContactList) {
                notify(getPersons());
            }

            for(Person person : toNotify) {
                notify(person);
            }

            for(Person person : toNotifyDialogs) {
                notify(dialogs.get(person));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAll(TelegramDAO telegramDAO){
        this.telegramDAO = telegramDAO;
        initialize();
        notify(this);
    }

    public void updateAll() {
        updateAll(this.telegramDAO);
    }

    private void notify(Object data) {
        try {
            observable.notifyObservers(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addObserver(Observer observer) {
        observable.addObserver(observer);
    }

    public void removeObserver(Observer observer) {
        observable.deleteObserver(observer);
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
        return d1 == d2 || d1 != null &&
                equals(d1.getBuddy(), d2.getBuddy()) &&
                d1.getUnreadCount() == d2.getUnreadCount() &&
                d1.getLastMessage().getId() == d2.getLastMessage().getId();
    }
}
