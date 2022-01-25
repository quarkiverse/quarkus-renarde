# Quarkus - Renarde

This is the Quarkus Renarde Web Framework.

You can read the (currently unrendered) [user documentation](/blob/main/docs/modules/ROOT/pages/index.adoc).

At the moment, Quarkus Renarde is not yet released, so you need to build it to try it out:

You will need to build a specific branch of Quarkus in order to use Quarkus Renarde at the moment (until all
patches are merged upstream):

```shell
$ git clone https://github.com/FroMage/quarkus.git
$ cd quarkus
$ git checkout renarde
$ mvn -T C1 -Dquickly
$ cd ..
```

Then you need to build Quarkus Renarde:

```shell
$ git clone https://github.com/quarkiverse/quarkus-renarde.git
$ cd quarkus-renarde
$ mvn clean install
$ cd ..
```

Please check out [a sample TODO application](https://github.com/FroMage/quarkus-renarde-todo) for inspiration.