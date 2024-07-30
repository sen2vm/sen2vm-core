# sen2vm-core
Sen2VM standalone tool that will be also called by Sen2VM SNAP Plugin

# How to run sen2vm-core

Inside `sen2vm-core` folder, run the next commands :

'''
mvn clean install
java -jar target/sen2vm-core-1.0-SNAPSHOT.jar -c [configuration_filepath] [-p [parameters_filepath]]
```

With example from current repository :

```
java -jar target/sen2vm-core-1.0-SNAPSHOT.jar -c src/test/resources/configuration_example.json -p src/test/resources/params.json