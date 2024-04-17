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

   - Move nginx to isolated cpu-s:

     1. Add `isolcpus` argument to kernel command-line parameters.
        
        For example (48 cores debian) 
        it can be done with `/etc/default/grub` config:

        ```
        # If you change this file, run 'update-grub' afterwards to update
        # /boot/grub/grub.cfg.
        # For full documentation of the options in this file, see:
        #   info -f grub -n 'Simple configuration'
        
        ...
        GRUB_CMDLINE_LINUX="... isolcpus=22-47"
        ...
        ```

        Then apply changes:
        ```bash
        update-grub
        ```

        And reboot machine:
        ```bash
        reboot
        ```

        Here we isolated 26 cores (22-47) from our debian.
        They are still visible,
        but os won't schedule any processes there 
        until explicitly stated otherwise.
        
        Also, it is highly recommended to take a look
        at cpu performance parameters, such as:
        - `cpufreq.default_governor` (recommended `performance`)
        - `processor.max_cstate` (recommended `1`)
        - `idle` (recommended `poll`)
        - `intel_pstate` (I used `disable`)
        - `intel_idle.max_cstate` (recommended `0`)

     2. Set up nginx workers count and affinity mask.
        
        Using `/etc/nginx/nginx.conf` we can set up workers settings:
        
        ```
        worker_processes 24;
        worker_cpu_affinity auto 111111111111111111111111000000000000000000000000;
        ```
        
        Here we set number of worker processes 
        to (isolated cores count - 2)
        And set their cpu affinity mask as 24-47 interval
        (higher cpu numbers first).

     3. Set up nginx master process cpu affinity

        Find nginx master process pid:
        
        ```bash
        pgrep -ox nginx
        ```

        Set master process cpu affinity using `taskset`:

        ```bash
        taskset -cp <pid> 22
        ```
     
        And restart nginx:

        ```bash
        nginx -s restart
        ```
     
        This procedure should be executed after every reboot.
     
     Finally, we have completely isolated nginx 
     with master process using cpu number 22 
     and 24 workers using cores 24-47, one per worker

2. Build

   - Nothing unusual, `mvn clean` + `mvn install`

3. Run benchmark

   - Benchmark uberjar named `benchmarks.jar` can be found in `target/` directory.

   - All benchmarks accept `-Dnginx_url` parameter
     used as a server address (default `localhost`), 
     see `com.example.benchmark.Utils.SERVER_URL`.
     
     Resulting url looks like `http://<nginx_url>/do_request`

   - Recommended run command:
     ```
     java \
       -Xmx32g -Xms32g \
       -jar benchmarks.jar \
       <benchmark class name>
     ```
