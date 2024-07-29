# sen2vm-core
Sen2VM standalone tool that will be also called by Sen2VM SNAP Plugin

# How to run sen2vm-core

Inside `sen2vm-core` folder, run the next commands :

```
mvn compile
mvn exec:java -Dexec.mainClass="esa.sen2vm.Sen2VM" -Dexec.args="-c src/test/resources/configuration_example.json -p src/test/resources/params.json"
```

