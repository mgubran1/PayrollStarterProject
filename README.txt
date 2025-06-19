Download: 

https://maven.apache.org/download.cgi

mvnd --version

cd C:\Users\Rayan\Desktop\Payroll
mvnd clean javafx:run

C:\mvnd\bin\mvnd.cmd clean javafx:run

C:\Program Files\apache-maven-3.9.10



How to fix
1. Install Maven
Download Maven from the official site: https://maven.apache.org/download.cgi

Extract the archive to a folder on your computer (e.g., C:\Program Files\apache-maven-3.9.6).

2. Add Maven to your PATH
Go to Control Panel > System > Advanced system settings > Environment Variables

Under "System Variables", find and select the Path variable, click Edit

Click New and add the bin directory inside your Maven folder, e.g.:

makefile
Copy
Edit
C:\Program Files\apache-maven-3.9.6\bin
Click OK to save and close all dialogs.

3. Verify Maven Installation
Open a new Command Prompt (important: open a NEW one after changing PATH)

Type:

nginx
Copy
Edit
mvn -version
You should see Maven version and Java version info.

4. Run Your Project
In your project directory, run:

arduino
Copy
Edit
mvn spring-boot:run
or

arduino
Copy
Edit
mvnw spring-boot:run
(if your project includes mvnw and mvnw.cmd files.)


How to add Maven to PATH (Windows 10/11):
Open Start Menu, search for “Environment Variables,” and select “Edit the system environment variables.”

Click the “Environment Variables” button.

Under System variables, scroll and select the variable called Path, then click Edit.

Click New, then paste in the Maven bin folder path:

makefile
Copy
Edit
C:\Users\Rayan\Desktop\apache-maven-3.9.10\bin
Click OK to close all dialogs.

Close all open terminals/PowerShell windows.

Open a new Command Prompt or PowerShell and run:

powershell
Copy
Edit
mvn -version
Now it should work from anywhere!