package io.quarkiverse.renarde.transporter.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import io.quarkiverse.renarde.transporter.DatabaseTransporter;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class TransporterTest {

    private final static String ORDERS = "$['" + Order.class.getName() + "']";
    private final static String USERS = "$['" + User.class.getName() + "']";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Order.class, User.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void testSerializer() {
        List<Order> orders = Arrays.asList(new Order(1l, "a"), new Order(2l, "b"));
        List<User> users = Arrays.asList(new User(1l, "a"), new User(2l, "b"));
        String export = DatabaseTransporter.export(orders, users);
        DocumentContext ctx = JsonPath.parse(export);
        System.err.println(export);
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + ".length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(ORDERS + "[0].id"));
        Assertions.assertEquals("a", ctx.read(ORDERS + "[0].name"));
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + "[1].id"));
        Assertions.assertEquals("b", ctx.read(ORDERS + "[1].name"));

        Assertions.assertEquals(2, (Integer) ctx.read(USERS + ".length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(USERS + "[0].id"));
        Assertions.assertEquals("a", ctx.read(USERS + "[0].name"));
        Assertions.assertEquals(2, (Integer) ctx.read(USERS + "[1].id"));
        Assertions.assertEquals("b", ctx.read(USERS + "[1].name"));

        Map<Class<?>, List<? extends PanacheEntityBase>> imported = DatabaseTransporter.importEntities(export);

        @SuppressWarnings("unchecked")
        List<Order> importedOrders = (List<Order>) imported.get(Order.class);
        Assertions.assertNotNull(importedOrders);
        Assertions.assertEquals(2, importedOrders.size());
        Assertions.assertEquals(1, importedOrders.get(0).id);
        Assertions.assertEquals("a", importedOrders.get(0).name);
        Assertions.assertEquals(orders.get(0).date, importedOrders.get(0).date);
        Assertions.assertEquals(2, importedOrders.get(1).id);
        Assertions.assertEquals("b", importedOrders.get(1).name);
        Assertions.assertEquals(orders.get(1).date, importedOrders.get(1).date);

        @SuppressWarnings("unchecked")
        List<User> importedUsers = (List<User>) imported.get(User.class);
        Assertions.assertNotNull(importedUsers);
        Assertions.assertEquals(2, importedUsers.size());
        Assertions.assertEquals(1, importedUsers.get(0).id);
        Assertions.assertEquals("a", importedUsers.get(0).name);
        Assertions.assertEquals(2, importedUsers.get(1).id);
        Assertions.assertEquals("b", importedUsers.get(1).name);
    }

    @Test
    public void testRelations() {
        List<Order> orders = Arrays.asList(new Order(1l, "a"), new Order(2l, "b"));
        List<User> users = Arrays.asList(new User(1l, "a"), new User(2l, "b"));
        orders.get(0).one = users.get(0);
        users.get(0).reverseOne.add(orders.get(0));
        orders.get(0).many.add(users.get(1));
        users.get(1).many.add(orders.get(0));

        String export = DatabaseTransporter.export(orders, users);
        DocumentContext ctx = JsonPath.parse(export);
        System.err.println(export);
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + ".length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(ORDERS + "[0].id"));
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + "[0].many[0].id"));
        Assertions.assertEquals(User.class.getName(), ctx.read(ORDERS + "[0].many[0]._type"));
        Assertions.assertEquals(1, (Integer) ctx.read(ORDERS + "[0].one.id"));
        Assertions.assertEquals(User.class.getName(), ctx.read(ORDERS + "[0].one._type"));
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + "[1].id"));
        Assertions.assertEquals(0, (Integer) ctx.read(ORDERS + "[1].many.length()"));

        Assertions.assertEquals(2, (Integer) ctx.read(USERS + ".length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(USERS + "[0].id"));
        Assertions.assertEquals("a", ctx.read(USERS + "[0].name"));
        Assertions.assertEquals(2, (Integer) ctx.read(USERS + "[1].id"));
        Assertions.assertEquals("b", ctx.read(USERS + "[1].name"));
        Assertions.assertEquals(1, (Integer) ctx.read(USERS + "[1].many.length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(USERS + "[1].many[0].id"));
        Assertions.assertEquals(Order.class.getName(), ctx.read(USERS + "[1].many[0]._type"));

        Map<Class<?>, List<? extends PanacheEntityBase>> imported = DatabaseTransporter.importEntities(export);

        @SuppressWarnings("unchecked")
        List<Order> importedOrders = (List<Order>) imported.get(Order.class);
        Assertions.assertNotNull(importedOrders);
        Assertions.assertEquals(2, importedOrders.size());
        Assertions.assertEquals(1, importedOrders.get(0).id);
        Assertions.assertEquals(2, importedOrders.get(1).id);

        @SuppressWarnings("unchecked")
        List<User> importedUsers = (List<User>) imported.get(User.class);
        Assertions.assertNotNull(importedUsers);
        Assertions.assertEquals(2, importedUsers.size());
        Assertions.assertEquals(1, importedUsers.get(0).id);
        Assertions.assertEquals(2, importedUsers.get(1).id);

        Assertions.assertEquals(importedUsers.get(0), importedOrders.get(0).one);
        Assertions.assertEquals(1, importedOrders.get(0).many.size());
        Assertions.assertEquals(importedUsers.get(1), importedOrders.get(0).many.get(0));
    }

    @Test
    public void testTransformer() {
        List<Order> orders = Arrays.asList(new Order(1l, "a"), new Order(2l, "b"));
        List<User> users = Arrays.asList(new User(1l, "a"), new User(2l, "b"));
        orders.get(0).one = users.get(0);
        users.get(0).reverseOne.add(orders.get(0));
        orders.get(0).many.add(users.get(1));
        users.get(1).many.add(orders.get(0));

        String export = DatabaseTransporter.export((entityType, attributeName, attributeValue) -> {
            if (entityType == Order.class) {
                switch (attributeName) {
                    case "name":
                        return attributeValue == null ? null : ((String) attributeValue).toUpperCase();
                    case "one":
                        return null;
                    case "many":
                        return null;
                }
            }
            return attributeValue;
        }, orders);
        DocumentContext ctx = JsonPath.parse(export);
        System.err.println(export);
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + ".length()"));
        Assertions.assertEquals(1, (Integer) ctx.read(ORDERS + "[0].id"));
        Assertions.assertEquals("A", ctx.read(ORDERS + "[0].name"));
        Assertions.assertThrows(PathNotFoundException.class, () -> ctx.read(ORDERS + "[0].many"));
        Assertions.assertThrows(PathNotFoundException.class, () -> ctx.read(ORDERS + "[0].one"));
        Assertions.assertEquals(2, (Integer) ctx.read(ORDERS + "[1].id"));
        Assertions.assertEquals("B", ctx.read(ORDERS + "[1].name"));
    }

    @Entity
    public static class Order extends PanacheEntity {
        public String name;
        // owning
        @JoinTable(name = "order_user", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "order_id"))
        @ManyToMany
        public List<User> many = new ArrayList<>();
        // owning
        @ManyToOne
        public User one;

        public Date date;

        public Order() {
        }

        public Order(Long id, String name) {
            this.id = id;
            this.name = name;
            this.date = new Date();
        }
    }

    @Entity
    public static class User extends PanacheEntity {
        public String name;
        // not owning
        @ManyToMany(mappedBy = "many")
        public List<Order> many = new ArrayList<>();
        // not owning
        @OneToMany(mappedBy = "one")
        public List<Order> reverseOne = new ArrayList<>();

        public User() {
        }

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
