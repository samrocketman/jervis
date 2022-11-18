Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/focal64"
  config.vm.box_check_update = false

  if Vagrant.has_plugin?("vagrant-vbguest") then
    config.vbguest.auto_update = false
  end

  config.vm.provider "virtualbox" do |vb|
    # Hardware settings
    vb.gui = true
    vb.cpus = "4"
    vb.memory = "8192"

    # Video Settings with remote desktop disabled and other settings
    vb.customize [ "modifyvm", :id,
      "--vram", "256",
      "--accelerate3d", "on",
      "--vrde", "off",
      "--graphicscontroller", "vboxsvga",
      "--nested-hw-virt", "on",
      "--clipboard-mode", "bidirectional"]
  end

  config.vm.provision "shell", inline: <<-SHELL
    set -exo pipefail
    whoami
    echo $SHELL
    export DEBIAN_FRONTEND=noninteractive

    # sleep anywhere between 1 and 9 seconds
    function random_sleep() {
      local interval="$(tr -dc '1-9' < /dev/urandom | head -c1)"
      sleep "$interval"
    }
    maxretries=300
    count=0
    function retry() {
      until ( "$@" ); do
        count=$((count + 1))
        if [ "$count" -gt "$maxretries" ]; then
          return 1
        fi
        random_sleep
      done
    }
    retry apt-get update
    retry apt-get install -y ca-certificates curl gnupg lsb-release
    mkdir -p /etc/apt/keyrings
    retry /bin/bash -exo pipefail -c 'curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg'
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
      > /etc/apt/sources.list.d/docker.list
    chmod a+r /etc/apt/keyrings/docker.gpg
    retry apt-get update
    retry apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    retry snap install --classic code
    retry apt-get install -y --no-install-recommends ubuntu-desktop gnome-startup-applications
    retry apt-get install -y --no-install-recommends virtualbox-guest-dkms virtualbox-guest-utils virtualbox-guest-x11
    # other applications
    retry apt-get install -y firefox git
    su - vagrant -c 'git config --global user.name "Your Name"'
    su - vagrant -c 'git config --global user.email "you@example.com"'
    mkdir -p ~vagrant/git
    retry git clone https://github.com/samrocketman/jervis.git ~vagrant/git/jervis
    usermod -a -G sudo vagrant
    usermod -a -G docker vagrant
    systemctl enable docker.service
    systemctl start docker.service

    retry curl -sSfLo /usr/local/bin/docker-compose https://github.com/docker/compose/releases/download/v2.12.2/docker-compose-linux-x86_64
    chmod 755 /usr/local/bin/docker-compose

    # wait for docker startup to complete
    until curl -sSfLo /dev/null --unix-socket /var/run/docker.sock http://localhost/info; do
      sleep 1
    done

    # startup automation
    retry su - vagrant -c 'code --install-extension ms-vscode-remote.remote-containers'
    retry su - vagrant -c 'code --install-extension ms-azuretools.vscode-docker'
    mkdir -p ~vagrant/.config/autostart
    cat > ~vagrant/.config/autostart/code.desktop <<'EOF'
[Desktop Entry]
Type=Application
Exec=code git/jervis
Hidden=false
NoDisplay=false
X-GNOME-Autostart-enabled=true
Name[C]=Open VSCode with Jervis
Name=Open VSCode with Jervis
Comment[C]=
Comment=
EOF

    mkdir -p ~vagrant/.config/Code/User/
    cat > ~vagrant/.config/Code/User/settings.json <<'EOF'
{
"workbench.startupEditor": "none",
    "telemetry.telemetryLevel": "off",
    "dev.containers.dockerComposePath": "/usr/local/bin/docker-compose"
}
EOF
    #auto login for vagrant
    mkdir -p /etc/gdm3/
    cat > /etc/gdm3/custom.conf <<'EOF'
[daemon]
AutomaticLoginEnable=True
AutomaticLogin=vagrant
[security]
[xdmcp]
[chooser]
[debug]
EOF

    # Disable Package upgrade notifications
    mkdir -p etc/update-manager/
    cat > /etc/update-manager/release-upgrades <<'EOF'
[DEFAULT]
Prompt=never
EOF
    cat > /etc/apt/apt.conf.d/10periodic <<'EOF'
APT::Periodic::Update-Package-Lists "0";
APT::Periodic::Download-Upgradeable-Packages "0";
APT::Periodic::AutocleanInterval "0";
APT::Periodic::Unattended-Upgrade "1";
EOF
    cp -f /etc/apt/apt.conf.d/10periodic /etc/apt/apt.conf.d/20auto-upgrades


    # Finalize and reboot to show VSCode automatically
    chown -R vagrant: ~vagrant
    retry apt-get upgrade -y
    retry snap refresh
    sudo reboot
  SHELL
end
