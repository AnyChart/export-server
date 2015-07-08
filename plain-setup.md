# Deploying

## Install required software (ubuntu trusty server)

Добавить в конец /etc/apt/sources.list:
```
deb http://nginx.org/packages/mainline/ubuntu/ trusty nginx
deb-src http://nginx.org/packages/mainline/ubuntu/ trusty nginx
```

Добавить ключ nginx:
```
wget http://nginx.org/keys/nginx_signing.key
apt-key add nginx_signing.key
rm nginx_signing.key
```

Обновить репы:
```
add-apt-repository ppa:webupd8team/java
apt-get update
```

Установить нужный софт:
```
apt-get install supervisor nginx oracle-java7-installer
```

Важно: нужна именно Oracle java, openjdk не канает

## Configure firewall
```
ufw allow ssh
ufw allow http
ufw enable
ufw status
```
status должен выдать такое:
```
Status: active

To                         Action      From
--                         ------      ----
22                         ALLOW       Anywhere
80                         ALLOW       Anywhere
22 (v6)                    ALLOW       Anywhere (v6)
80 (v6)                    ALLOW       Anywhere (v6)
```

# App structure
```
mkdir /apps
mkdir /apps/export
```

# supervisor configuration
Создаем файл `/etc/supervisor/conf.d/export.conf`
```
[program:export]
command=java -jar /apps/export/anychart-export-server-standalone.jar
stdout_logfile=/var/log/supervisor/export.out.log
stderr_logfile=/var/log/supervisor/export.err.log
```

Применяем изменения:
```
supervisorctl reread
supervisorctl update
supervisorctl status
```
Выдаст:
```
root@vm89445:~# supervisorctl reread
export: available
root@vm89445:~# supervisorctl update
export: added process group
root@vm89445:~# supervisorctl status
export                             BACKOFF    Exited too quickly (process log may have details)
```
`BACKOFF` это нормально - мы еще не выложили приложение.

# nginx configuration

Создаем `/etc/nginx/conf.d/export.anychart.com.conf`
```
upstream http_backend {
    server 127.0.0.1:49050;
    keepalive 32;  # both http-kit and nginx are good at concurrency
}

server {
    listen       80;
    server_name  export.anychart.com;

    location / {
        proxy_pass  http://http_backend;

        # tell http-kit to keep the connection
        proxy_http_version 1.1;
        proxy_set_header Connection "";

        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Host $http_host;
    }
}
```

Перезапускаем nginx:
```
service nginx configtest
service nginx restart
```
Должен выдать:
```
root@vm89445:~# service nginx configtest
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
root@vm89445:~# service nginx restart
 * Restarting nginx nginx        [ OK ]
 ```

# Buildong export server
Собираем (ЛОКАЛЬНО У СЕБЯ) движок.
Для сборки требуется leiningen (гуглится)

Собираем:
```
lein uberjar
lein localrepo coords lib/xercesImpl_2.11.0.jar
lein localrepo install lib/xercesImpl_2.11.0.jar xerces/xercesImpl 2.11.0
lein localrepo coords lib/org.eclipse.wst.xml.xpath2.processor_1.1.0.jar
lein localrepo install lib/org.eclipse.wst.xml.xpath2.processor_1.1.0.jar org.eclipse.wst.xml.xpath2.processor 1.1.0
lein uberjar
```

Должен отдать что-то вроде:
```
Created /Users/alex/Work/anychart/export-server/target/uberjar/anychart-export-server.jar
Created /Users/alex/Work/anychart/export-server/target/uberjar/anychart-export-server-standalone.jar
```

# Manual deploy
Подключаемся к серверу по sftp (в отдельном окошке) и заливаем получившуюся wiki-1.1-standalone.jar:
```
sftp root@server-ip
sftp> cd /apps/export
sftp> put /Users/alex/Work/anychart/export-server/target/uberjar/anychart-export-server-standalone.jar
```

# Запускаем движок export:
По ssh:
```
supervisorctl
export                             FATAL      Exited too quickly (process log may have details)
supervisor> start export
export: started
supervisor> status
export                             RUNNING    pid 14435, uptime 0:00:02
```
И ждем порядка 5 секунд (запуск)

# Обновляем dns
Обновляем dns на 1and1
