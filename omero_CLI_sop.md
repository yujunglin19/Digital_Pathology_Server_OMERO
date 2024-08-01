# OMERO CLI SOP

## Installation

 If the user has direct access to OMERO-server, i.e. server administrators, creating a new virtual environment is not required. Server administrators can activate OMERO-server venv by running `/opt/omero/server/venv3/bin/activate` and follow option 2 to install omero-py.
 
 If one does not have access to OMERO-server, there are two options for them to create a new virtual environment and install omero-py.
    
- Option 1: Conda environment

    ```
    conda create -n myenv -c conda-forge python=3.8 omero-py
    conda activate myenv
    ```

- Option 2: Python venv
    
    ```
    python -m venv myenv
    . myenv/bin/activate
    pip install omero-py
    ```

After installing omero-py using one of the options above, the user can now access omero commands.

To access the omero-server, run `omero login` and input login information.
    
### Markdown Editor License

MIT

**Free Software, Hell Yeah!**
