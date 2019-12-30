1)Prequisites:
Add these three lines which are our servers for scenario to /etc/hosts of every single servers(the whole four of them)
```
192.168.1.30 myjenkins #Ansible,Maven installed
192.168.1.29 mygitlab
192.168.1.90 myserver #docker-ce,python-pip installed
192.168.1.10 mypc #where I write codes and manage servers
```

2)Passwordless connection preparation

   *A)ssh to "myjenkins" and change jenkins shell from `/bin/false` to `/bin/bash` in `/etc/passwd`
   
   *B)change user to jenkins `su - jenkins`
   
   *C)issue `ssh-keygen` and follow the interactive questions
   
   *D)issue `ssh-copy-id -i ~/.ssh/id_rsa.pub root@myserver` and bring the requierd password (of myserver)

3)Create new Repository 

   *A)Login to the gitlab with your username/password
   
   *B)Create a repo like below :

![1st](/pix/gitlab/01-gitlab_landing_page.png)
![2nd](/pix/gitlab/02-Create_a_project.png)
![3rd](/pix/gitlab/03-new_project_fill.png)


4) Write our java-springboot sample code :
create a folder for your project, in my case I named it myproject
```
mkdir myproject/
cd myproject
mkdir -p src/main/java/hello/
touch src/main/java/hello/HelloController.java
```
and then copy-paste below content into touched file.

```
package hello;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/")
        public String index() {
            return "GWF was here\n";
        }
    @RequestMapping("/hello")
        public String index2() {

            return "This is the hello world context!!!\n";
        }

}
```



```
touch src/main/java/hello/MainApplicationClass.java
```
and then copy-paste below content into touched file.

```
package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainApplicationClass {

    public static void main(String[] args) {
        SpringApplication.run(MainApplicationClass.class, args);
    }
}
```



Create a file named "pom.xml" with following content which is needed for the maven to build our application (Note: pom.xml should be in the root of your project folder)

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>hello</groupId>
    <artifactId>myapp</artifactId>
    <version>1.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.9.RELEASE</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```


5) Create our Dockerfile and name it exactly "Dockerfile" with below content :

```
FROM alpine
USER root
RUN apk --update add openjdk11
COPY myapp-1.0.jar /
```

Explanation: do not panic , the file myapp-1.0.jar will be created by maven after successfull build.

6) Create our ansible yaml file for deployment which in my case is named "java_deployment.yaml" with following content


```---
- hosts: javasrvs
  name: java_deployments
  tasks: 
    - name: copy to destintion(server)
      copy:
        src: /var/lib/jenkins/workspace/springboot_docker/target/myapp-1.0.jar
        dest: /root/
        owner: root
        group: root
        mode: '0755'
        
    - name: Copy dockerfile    
      copy:
        src: /var/lib/jenkins/workspace/springboot_docker/Dockerfile
        dest: /root/
        owner: root
        group: root
        mode: '0755'
    
    - name: test the current path and whoami
      shell: pwd > curpa.txt && whoami >> curpa.txt

    - name: installing "docker-py with pip"
      pip: 
        name: docker-py

    - name: dockerfile
      docker_image: 
        path: /root 
        name: mydocker_image 
        state: present
    
    - name: running container
      docker_container:
        name: mydocker_container
        image: mydocker_image:latest
        state: started
        command: ["java", "-jar", "/myapp-1.0.jar"]
        ports: 
          - "12345:8080"
    - name: remove external file(Dockerfile)
      file: 
        path: /root/{{ item }}
        state: absent
      loop:
        - Dockerfile
        - myapp-1.0.jar
```



7) Create hosts.ini for ansible in the same folder as "java_deployment.yaml" with following content :

```[javasrvs]
myserver ansible_host=192.168.1.90 ansible_user=root
```

8) Create Jenkinsfile with the exact name and following content :

```
pipeline {
    agent any
    stages {
        stage('gitfecth') {
            steps {
                checkout scm
            }
        }
        stage('maventest') {
            steps {
                sh 'mvn test'
            }
        }
        stage('mavenbuild') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('deployment_ansible') {
            steps {
                sh 'ansible-playbook -i hosts.ini java_deployment.yaml'
            }
        }
    }
}

```

9) Push everything to git!
issue the following commands from your current folder in "mypc" and from the path "myproject"


```
git config --global user.name "myusername"
git config --global user.email "myusername@me.com"
cd myproject
git init
git remote add origin http://mygitlab/myusername/springboot_docker.git
git add .
git commit -m "Initial commit"
git push -u origin master
```

10) Now we are finished with gitlab,ansible and codes and it is the time for Jenkins,so please follow the pictures to create new Pipeline Item .

![1st](/pix/jenkins/01-landing_page.png)
![2nd](/pix/jenkins/02-New_Item.png)
![3rd](/pix/jenkins/03-Pipeline_script_from_SCM.png)
![4th](/pix/jenkins/04-SCM.png)
![5th](/pix/jenkins/05-add_Credential.png)
![6th](/pix/jenkins/06-Add_Credentials_my_gitlab_userpass.png)
![7th](/pix/jenkins/07-Build_Now.png)
![8th](/pix/jenkins/08-Built.png)


11) issue following command on "mypc" as below

`curl http://myserver:12345/hello`
and the result should be :
`This is the hello world context!!!`

which means you did the job! 
Have fun!
