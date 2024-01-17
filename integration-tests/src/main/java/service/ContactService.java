package service;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContactService {
    private static final Contact JOE = new Contact(6, "Joe", "BLOW", "joe@blow.io");
    private static final Contact FOO = new Contact(7, "Foo", "BLOW", "foo@blow.io");
    private static final Contact BAR = new Contact(10, "Bar", "AWESOME", "bar@awesome.io");

    private final Map<Integer, Contact> contacts = new HashMap<>();

    public ContactService() {
        reset();
    }

    public Map<Integer, Contact> contacts() {
        return contacts;
    }

    public void reset() {
        contacts.clear();
        contacts.put(JOE.id, JOE.clone());
        contacts.put(FOO.id, FOO.clone());
        contacts.put(BAR.id, BAR.clone());
    }

    public static class Contact {
        public final int id;
        public String firstName;
        public String lastName;
        public String email;

        public boolean locked = false;

        public Contact(int id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        @Override
        public Contact clone() {
            return new Contact(id, firstName, lastName, email);
        }
    }
}
