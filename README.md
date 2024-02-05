## HTTP Client benchmarks

### Setup

1. Nginx 

   - Install nginx (apt for linux, homebrew for macos)

     - Set up endpoint, example configuration to be placed in `http` section:
       ```
       server {
           listen 8080 default_server;

           location /do_request {
               return 200 'You are welcome';
           }
       }
       ```
     - Disable logging and default pages
       ```
       # include /etc/nginx/conf.d/*.conf;
       # include /etc/nginx/sites-enabled/*;
       ```
       ```
       # access_log /var/log/nginx/access.log;
       access_log off;
       ```

2. Build

   - Nothing unusual, `mvn clean` + `mvn install`

3. Run benchmark

   - Benchmark uberjar named `benchmarks.jar` can be found in `target/` directory.

   - Recommended run command:
     ```
     java \
       --add-opens java.base/java.lang=ALL-UNNAMED \
       --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
       --add-opens java.base/java.util.concurrent.locks=ALL-UNNAMED \
       -Xmx32g -Xms32g \
       -jar benchmarks.jar \
       <benchmark class name>
     ```
