# OMERO Web Setup SOP

Please note that this SOP is for setting up of OMERO web on Linux system (Ubuntu 22.04). It is not compatible with other versions of Ubuntu or systems due to the differences in commands used. Please make sure OMERO.server is installed before OMERO.web.

## Installation

All package installations should be run as root in order to gain access to certain files. One can run the commands with `sudo` or switch to root user by running `sudo -i`.

    
- Create omero-web system user
    
    ```
    useradd -p web-password -m omero-web
    mkdir -p /opt/omero/web/omero-web/etc/grid
    chown -R omero-web /opt/omero/web/omero-web
    ```
    
    One can change the password to their own value.
    
- Install dependencies

    Dependencies installation should be run as the root user.
    
    ```
    apt-get update
    apt-get -y install unzip
    apt-get -y install python3
    apt-get -y install python3-venv
    apt-get -y install nginx
    
    # installation of Redis is optional
    apt-get -y install redis-server
    service redis-server start
    ```
    
    Installing Redis is optional. If one wishes to use Redis Cache, run the last command to install Redis.
    
- Install OMERO.web and IcePy

    It is recommended to install OMERO.web in a virtual environment to avoid any potential package version conflict. 
    ```
    python3 -mvenv /opt/omero/web/venv3
    /opt/omero/web/venv3/bin/pip install https://github.com/glencoesoftware/zeroc-ice-py-ubuntu2204-x86_64/releases/download/20221004/zeroc_ice-3.6.5-cp310-cp310-linux_x86_64.whl
    /opt/omero/web/venv3/bin/pip install --upgrade pip
    /opt/omero/web/venv3/bin/pip install omero-web
    # optional
    /opt/omero/web/venv3/bin/pip install 'django-redis==5.0.0'
    ```
    
- OMERO.web configuration

    All configuration options can be found at [Web developers documentation](https://merge-ci.openmicroscopy.org/jenkins/job/OMERO-docs/lastBuild/artifact/omero/_build/html/developers/index.html#web-index). Below are some basic configurations.
    
    The following commands are run as the omero-web system user unless specified.
    
    For an easier configuration process, store the configuration option as environment variables.
    
    ```
    export WEBSESSION=True
    export OMERODIR=/opt/omero/web/omero-web
    export PATH=/opt/omero/web/venv3/bin:$PATH
    ```
    
    - configure NGINX
        
        ```
        omero web config nginx --http "${WEBPORT}" --servername "${WEBSERVER_NAME}" > /opt/omero/web/omero-web/nginx.conf.tmp
        ```

    - configure Redis if Redis is installed
        
        ```
        omero config set omero.web.caches '{"default": {"BACKEND": "django_redis.cache.
        RedisCache", "LOCATION": "redis://127.0.0.1:6379/0"}}'
        omero config set omero.web.session_engine django.contrib.sessions.backends.cache
        ```

    - configure Gunicorn
        
        ```
        omero.web.wsgi_workers (2 x NUM_CORES) + 1 # depends on usage
        omero config set omero.web.wsgi_args -- "--log-level=DEBUG --error-logfile=/opt/omero/web/omero-web/var/log/error.log"
        ```
    
    - configure CORS
        
        ```
        omero config append omero.web.middleware '{"index": 0.5, "class": "corsheaders.middleware.CorsMiddleware"}'
        omero config append omero.web.middleware '{"index": 10, "class": "corsheaders.middleware.CorsPostCsrfMiddleware"}'
        omero config set omero.web.cors_origin_whitelist '["https://hostname.example.com"]' # change to include desired websites
        # or to allow all
        omero config set omero.web.cors_origin_allow_all True
        ```
        
        It is important to keep the order of configuration of CORS. `CorsMiddleware` is always the first and `CorsPostCsrfMiddleware` is always the last.
    
    - configure WhiteNoise
        
        ```
        omero config append -- omero.web.middleware '{"index": 0, "class": "whitenoise.middleware.WhiteNoiseMiddleware"}'
        ```
    
    - configure NGINX

        Configuration of NGINX is run as the root user.
        
        ```
        sed -i.bak -re 's/( default_server.*)/; #\1/' /etc/nginx/nginx.conf
        rm /etc/nginx/sites-enabled/default
        cp /opt/omero/web/omero-web/nginx.conf.tmp /etc/nginx/conf.d/omeroweb.conf
        service nginx start
        ```
    
    Restart OMERO web using `omero web restart` to apply changes.
    
    If one decided to configure `omero.web.server_list`, the correct notation is to use double quotes. In order to get `[["localhost",4064,"omero"]]`, the input string should be `["[\"localhost\",4064,\"omero\"]"]`.
    
    - test installation

        To test if OMERO.web is correctly installed, run the following commands.
        
        ```
        omero web start
        omero web stop
        ```
    
- OMERO.web extensions

    There are a few applications that can be added to OMERO-web. The configuration processes for OMERO-figure, OMERO-iviewer, and OMERO-fpbioimage are summerized in this section.
    
    - OMERO-figure
    
        ```
        pip install -U omero-figure
        omero config append omero.web.apps '"omero_figure"'
        omero config append omero.web.ui.top_links '["Figure", "figure_index", {"title": "Open Figure in new tab", "target": "_blank"}]'
        omero config append omero.web.open_with '["omero_figure", "new_figure", {"supported_objects":["images"], "target": "_blank", "label": "OMERO.figure"}]' # allow users to open image file with omero-figure
        ```
    - OMERO-iviewer
    
        ```
        pip install -U omero-iviewer
        omero config append omero.web.apps '"omero_iviewer"'
        omero config set omero.web.viewer.view omero_iviewer.views.index # replace default viewer
        omero config append omero.web.open_with '["omero_iviewer", "omero_iviewer_index", {"supported_objects":["images", "dataset", "well"], "script_url": "omero_iviewer/openwith.js", "label": "OMERO.iviewer"}]'
        omero config set omero.pixeldata.max_projection_bytes # set image limit
        omero config set omero.web.iviewer.roi_color_palette "[rgb(0,255,0)],[darkred,red,pink],[#0000FF]" # color 
        omero config set omero.web.iviewer.show_palette_only true
        ```
    - OMERO-fpbioimage
        ```
        pip install -U omero-fpbioimage`
        omero config append omero.web.apps '"omero_fpbioimage"'
        omero config append omero.web.open_with '["omero_fpbioimage", "fpbioimage_index", {"script_url": "fpbioimage/openwith.js", "supported_objects": ["image"], "label": "FPBioimage"}]'
        ```

### Markdown Editor License

MIT

**Free Software, Hell Yeah!**
