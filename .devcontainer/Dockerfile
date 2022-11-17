# [Choice] Java version (use -bullseye variants on local arm64/Apple Silicon): 11, 17, 11-bullseye, 17-bullseye, 11-buster, 17-buster
FROM ubuntu:20.04

RUN set -ex; \
  export DEBIAN_FRONTEND=noninteractive; \
  apt-get update; \
  apt-get install -y openjdk-11-jdk git firefox curl unzip zip; \
  yes | adduser vscode

USER vscode
WORKDIR /home/vscode
ENV USER=vscode HOME=/home/vscode
RUN \
  curl -sL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.2/install.sh  | bash; \
  /bin/bash -c 'source ~/.nvm/nvm.sh; nvm install node'
CMD ["/bin/bash", "-c", "while sleep 1000;do true;done"]
