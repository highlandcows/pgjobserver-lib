FROM postgres:14.1

COPY ephemeralpg-3.1.tar.gz /tmp

COPY entrypoint.sh /entrypoint.sh

RUN \
  apt-get update -y && \
  apt-get -y install apt-transport-https curl gnupg make sudo unzip zip

RUN \
  apt-get update -y && \
  apt-get -y install gcc

RUN \
  apt-get update -y && \
  apt-get -y install openjdk-11-jdk

RUN \
  echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list && \
  echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list && \
  curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo -H gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import && \
  chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg && \
  apt-get update -y && sudo apt-get install -y sbt

RUN \
  cd /tmp && \
  tar xzf ephemeralpg-3.1.tar.gz && \
  cd ephemeralpg-3.1 && \
  make install

ENTRYPOINT ["/entrypoint.sh"]