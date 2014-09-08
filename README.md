cloudfeeds-atomhopper
=====================

**Cloud Feeds Atom Hopper**
is a Cloud Feeds component that customizes Atom Hopper web application for Rackspace use.
Some of the customizations are:

* adding a servlet filter that inserts tenantId search category on tenanted requests

**How to build**
```
mvn clean install
```

**How to build an RPM**
```
mvn -P build-rpm clean install
```
