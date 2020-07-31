Role Name
=========

This role installs and configures jenkins on a centos7 machine.

Requirements
------------


Role Variables
--------------

    jenkins_version: '2.235.3'
    jenkins_version: 'latest'
Set the jenkins version to install.
The latest tag is set by the public Jenkins repository: http://mirrors.jenkins.io/war-stable/


    jenkins_plugins: '{{ jenkins_default_plugins }}'
List of plugin short names to install in this jenkins instance


    jenkins_update_plugins : 'yes'
Set to 'yes' to update already installed plugins.


    jenkins_download_baseurl: '' (mandatory)
URL to a directory containing jenkins .war files


    jenkins_download_file: 'jenkins_{{ jenkins_version }}'
Defines the name of the .war file to download.


    jenkins_download_folder: '/tmp'
Defines the directory on the destination server, where the .war file will be downloaded


    jenkins_home: '/home/{{ jenkins_user }}/.jenkins'
Defines the jenkins home directory where jenkins stores its data


    jenkins_base_url: 'https://{{ inventory_hostname }}{{ jenkins_webappcontextpath }}'
Defines the base url of this jenkins installation


    jenkins_webappcontextpath: '/jenkins'
Defines jenkins context path, f.e. for a reverse proxy setup.
Set jenkins_webappcontextpath to '/' if no context path is wanted.


    jenkins_admin_username               : ''
    jenkins_admin_password               : ''
Define the username and password of the local jenkins administrator


    jenkins_ad_admin_username            : ''
    jenkins_ad_admin_password            : ''
Defines the active directory username and password.
If you install active directory plugin, login with local admin account gets impossible, so an active directory user is needed

Dependencies
------------

role dependencies:
- inverso.java

Example Playbook
----------------

Including an example of how to use your role (for instance, with variables passed in as parameters) is always nice for users too:

    - hosts: jenkins
      roles:
         - role: inverso.jenkins
           jenkins_version: '2.235.3'
           jenkins_download_baseurl: 'http://mirrors.jenkins.io/war-stable/{{ jenkins_version }}'
           jenkins_download_file: 'jenkins.war'
           jenkins_update_plugins: 'yes'
           jenkins_admin_username: 'jenkins-admin'
           jenkins_admin_password: 'vault_me'
           

License
-------

BSD
