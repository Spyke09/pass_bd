# Компиляция и запуск

Чтоб не заморачиваться, компилировать можно при помощи IntellijIdea

* ```Ctrl+Shift+F9```

* или ![](https://resources.jetbrains.com/help/img/idea/2023.1/app.toolwindows.toolWindowBuild_dark.svg)
 сверху справа

Сервер и клиент нужно запускать в разных терминалах.

Команда для запуска сервера

```java-path -classpath "out\production\Server;out\production\Package;out\production\DataBase;DataBase\src\sqlite-jdbc-3.41.0.0.jar" MainServer```

Команда для запуска клиента.

```java-path -classpath "out\production\Client;out\production\Package" MainClient```