# Local OMERO Server Setup SOP

Please note that this SOP is for setting up a local OMERO server on a Linux environment (Ubuntu 22.04). It is not compatible with other versions of Ubuntu or systems due to the differences in commands used.

**1. Installation**

Dependency installations should be run as the root user in order to gain access to certain files. One can run the installation commands with `sudo` or run `sudo -i` to switch to the root user.
For easier installation, the omero-server system user and the main OMERO configuration options are defined as environment variables and stored in `settings.env`. Below is the content of the file. One can change the values to their values. The file can also be downloaded from [here](settings.env).

```
OMERO_DB_USER=db_user
OMERO_DB_PASS=db_password
OMERO_DB_NAME=omero_database
OMERO_ROOT_PASS=omero-root
OMERO_DATA_DIR=/OMERO

export OMERO_DB_USER OMERO_DB_PASS OMERO_DB_NAME OMERO_ROOT_PASS OMERO_DATA_DIR

export PGPASSWORD="$OMERO_DB_PASS"

# Location of the OMERO.server
export OMERODIR=/opt/omero/server/OMERO.server

# Location of the virtual environment for omero-py
VENV_SERVER=/opt/omero/server/venv3

export ICE_HOME=/opt/ice-3.6.5		

export PATH=$ICE_HOME/bin:$VENV_SERVER/bin:$PATH
```
    
If one wants to use a local folder as the data directory, one can change the value of `OMERO_DATA_DIR` to the path of the folder. `Settings.env` needs to be sourced every time the system user is switched.
- Java 11 installation

    ```
    apt-get update
    apt-get -y install unzip wget bc
    apt-get -y install cron
    apt-get update -q
    apt-get install -y openjdk-11-jre
    ```
 - Ice installation

    ```
    apt-get update && \
    apt-get install -y -q \
    db5.3-util bzip2 libdb++ libexpat1 libmcpp0 openssl mcpp zlib1g
    
    cd /tmp
    wget -q https://github.com/glencoesoftware/zeroc-ice-ubuntu2204-x86_64/releases/download/20221004/Ice-3.6.5-ubuntu2204-x86_64.tar.gz
    tar xf Ice-3.6.5-ubuntu2204-x86_64.tar.gz
    mv Ice-3.6.5 /opt/ice-3.6.5
    echo /opt/ice-3.6.5/lib64 > /etc/ld.so.conf.d/ice-x86_64.conf
    ldconfig
    ```
    
    If one encounters permission denial when running `>`, change the command to `sudo echo ... | sudo tee path/to/file`.
    
    To make Ice available to all system users, configure `/etc/profile`:
    
    ```
    export ICE_HOME=/opt/ice-3.6.5		
    export PATH="$ICE_HOME/bin:$PATH"		
    export SLICEPATH="$ICE_HOME/slice"
    ```

    Next, add the virtual environment to `PATH`:
    
    ```
    VENV_SERVER=/opt/omero/server/venv3
    export ICE_HOME=/opt/ice-3.6.5		
    export PATH=$ICE_HOME/bin:$VENV_SERVER/bin:$PATH
    ```

- PostgreSQL installation

    ```
    apt-get install -y gnupg
    echo "deb [arch=amd64] http://apt.postgresql.org/pub/repos/apt jammy-pgdg main" > /etc/apt/sources.list.d/pgdg.list
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
    apt-get update
    apt-get -y install postgresql-15
    service postgresql start
    ```
    
    One might see a warning for `apt-key`, it can be ignored for now since it will not affect the server installation.
    
- Create OMERO server and PostgreSQL system user

    ```
    useradd -p omero-password -mr omero-server
    chmod a+X ~omero-server
    ```
    
    One can change the password to their own value. After creating the server user, one can make `settings.env` to the server system user by copying the file to the user's home directory. For the user to run omero command, `settings.env` needs to be sourced every time one switches users. One can source the env file by running `source [script]` or `. [script]`
    
    If one did not source the file, `mkdir -p $OMERO_DATA_DIR` in OMERO.server installation step will give an error message. It is better to use the absolute path when sourcing the file to avoid possible file-not-found errors. The absolute path can be obtained by `cd` to the directory containing the file and then running `pwd`.
    
    ```
    mkdir -p "$OMERO_DATA_DIR"
    chown omero-server "$OMERO_DATA_DIR"
    echo "CREATE USER $OMERO_DB_USER PASSWORD '$OMERO_DB_PASS'" | su - postgres -c psql
    su - postgres -c "createdb -E UTF8 -O '$OMERO_DB_USER' '$OMERO_DB_NAME'"
    psql -P pager=off -h localhost -U "$OMERO_DB_USER" -l
    ```
    
- Install OMERO.server

    OMERO.server installation needs to be run as root user.

    ```
    python3 -mvenv $VENV_SERVER
    $VENV_SERVER/bin/pip install --upgrade pip
    $VENV_SERVER/bin/pip install https://github.com/glencoesoftware/zeroc-ice-py-ubuntu2204-x86_64/releases/download/20221004/zeroc_ice-3.6.5-cp310-cp310-linux_x86_64.whl
    $VENV_SERVER/bin/pip install omero-server
    cd /opt/omero/server
    SERVER=https://downloads.openmicroscopy.org/omero/5.6/server-ice36.zip
    wget -q $SERVER -O OMERO.server-ice36.zip
    unzip -q OMERO.server*
    chown -R omero-server OMERO.server-*
    ln -s OMERO.server-*/ OMERO.server
    ```
    
 - Configuring OMERO.server 

    Remember to source `settings.env` before configuring the server.
    The following command will configure the database and data directory.
    ```
    omero config set omero.data.dir "$OMERO_DATA_DIR"
    omero config set omero.db.name "$OMERO_DB_NAME"
    omero config set omero.db.user "$OMERO_DB_USER"
    omero config set omero.db.pass "$OMERO_DB_PASS"
    omero db script -f $OMERODIR/db.sql --password "$OMERO_ROOT_PASS"
    psql -h localhost -U "$OMERO_DB_USER" "$OMERO_DB_NAME" < $OMERODIR/db.sql
    ```
    
- Certificates

    To connect to an OMERO.server using OMERO clients (CLI, OMERO.web), a self-signed certificates should be generated by running:
    ```
    omero certificates
    ```
    
- Running the server

    After switching to omero-server user and sourcing the settings.env file. It is recommended to run `omero certificates` every time to update the certification.
    To start the server, run
    ```
    omero admin start
    ```
    The server needs to be running for users to connect through webclient portal.
    The default connection number of the server is set to 10 when it is first set up. If one wishes to increase the performance, one can first check the [requirements](https://merge-ci.openmicroscopy.org/jenkins/job/OMERO-docs/lastBuild/artifact/omero/_build/html/sysadmins/system-requirements.html) to make sure the hardware is capable of handling the server. Then, run `omero config set omero.db.poolsize` to configure the server.
    To view the current setup of the server, one can run `omero admin diagnostics`. To view the configurations, one can run `omero config get`.

### Markdown Editor License

MIT

**Free Software, Hell Yeah!**
