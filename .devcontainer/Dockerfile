# [Choice] Java version (use -bullseye variants on local arm64/Apple Silicon): 11, 17, 11-bullseye, 17-bullseye, 11-buster, 17-buster
FROM ubuntu:20.04

# JDK 11 or JDK 17
ARG JDK=11

SHELL ["/bin/bash", "-exc"]
RUN \
  export DEBIAN_FRONTEND=noninteractive; \
  apt-get update; \
  apt-get install -y openjdk-"${JDK}"-jdk git firefox curl unzip zip gpg; \
  yes | adduser vscode

USER vscode
WORKDIR /home/vscode
ENV USER=vscode HOME=/home/vscode
RUN \
  curl -sL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.2/install.sh  | bash; \
  /bin/bash -c 'source ~/.nvm/nvm.sh; nvm install node'
RUN set -ex; \
mkdir ~/groovydoc; \
echo '<html><head><title>Jervis API Documentation</title><style>tt { font-size: 1.5em; font-weight: bold; }</style></head><body><h2>Getting Started</h2><p>In VSCode, go to the file <tt>Explorer</tt>, under <tt>Quick Run</tt>, choose to run <tt>Generate API Docs</tt>.</p><p>Refresh this page.</p></body></html>' > ~/groovydoc/index.html

CMD ["/bin/bash", "-c", "while sleep 1000;do true;done"]
