Se envia por POST a /UploadFile un archivo del tipo .pdf, .txt, .docx

Se devuelve un JSON con los campos:
- apellido
- nombre
- email
- telefono
- texto

En caso de que alguna de esas propiedades no se encuentren en el archivo entregado, no serán partes del JSON de salida


Instalación Tomcat corretto8

Comandos:

$ sudo amazon-linux-extras enable corretto8

$ sudo yum clean metadata

$ sudo yum install -y java-1.8.0-amazon-corretto

Verificar que la versión de Java sea correcta

$ java -version
openjdk version "1.8.0_232"
OpenJDK Runtime Environment Corretto-8.232.09.1 (build 1.8.0_232-b09)
OpenJDK 64-Bit Server VM Corretto-8.232.09.1 (build 25.232-b09, mixed mode


Instale Tomcat 8.5 desde amazon-linux-extras:

$ sudo amazon-linux-extras enable tomcat8.5
$ sudo yum clean metadata
$ sudo yum install -y tomcat

Configure el tomcat para usar un directorio como entropy source:

$ sudo bash -c 'echo JAVA_OPTS=\"-Djava.security.egd=file:/dev/urandom\" >> /etc/tomcat/tomcat.conf'

Cree la carpeta raíz para el tomcat:
$ sudo install -d -o root -g tomcat /var/lib/tomcat/webapps/ROOT 

Utilice WinSCP para mover el archivo .war hasta el directorio "/var/lib/tomcat/webapps/"

y Finalmente, inicie el servicio del tomcat

$ sudo systemctl start tomcat 
