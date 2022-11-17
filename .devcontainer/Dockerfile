# [Choice] Java version (use -bullseye variants on local arm64/Apple Silicon): 11, 17, 11-bullseye, 17-bullseye, 11-buster, 17-buster
FROM ubuntu:20.04
ENV DEBIAN_FRONTEND=noninteractive

RUN set -ex; \
  apt-get update; \
  apt-get install -y openjdk-11-jdk git; \
  yes | adduser vscode

USER vscode
WORKDIR /home/vscode
ENV USER=vscode HOME=/home/vscode
CMD ["/bin/bash", "-c", "while sleep 1000;do true;done"]
