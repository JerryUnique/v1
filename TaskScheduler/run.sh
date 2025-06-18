#!/bin/bash

# 构建项目
mvn clean package -DskipTests

# 运行项目
java -jar target/task-scheduler-1.0.0.jar