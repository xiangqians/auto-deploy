@title Automatic Deployment
@echo on
java -Dfile.encoding=utf-8 -Xss4096K -Xms512M -Xmx512M -jar ${project.build.finalName}.jar
pause
