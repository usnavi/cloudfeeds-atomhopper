cloudfeeds-atomhopper
=====================

**Cloud Feeds Atom Hopper**
is a Cloud Feeds component that customizes Atom Hopper web application for Rackspace use.
Some of the customizations are:

* Xml2JsonFilter: a servlet filter that converts XML to JSON on "application/vnd.rackspace.atom+json" ```Accept``` header
* TenantedEntryVerificationFilter: a servlet filter that ensures the tenantId in the URI matches with the tenantId inside the requested Atom entry via the URI /<feedname>/events/<tenantId>/entries/urn:uuid:<entryId>
* PrivateAttrsFilter: a servlet filter that removes private attributes of certain events/feeds for observers
* ExternalHrefFilter: a servlet filter that resolves the links (header and atom:link) correctly for requests coming in from external Cloud Feeds nodes
* TenantedFitler: a servlet filter that inserts tenantId search category and remove the tenantId from URI
  on tenanted requests

**How to build**
```
mvn clean install
```

**How to build an RPM**
```
mvn -P build-rpm clean install
```
