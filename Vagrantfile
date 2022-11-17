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
    apt-get update
    apt-get install -y ca-certificates curl gnupg lsb-release
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
      > /etc/apt/sources.list.d/docker.list
    chmod a+r /etc/apt/keyrings/docker.gpg
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    snap install --classic code
    apt-get install -y --no-install-recommends ubuntu-desktop gnome-startup-applications
    apt-get install -y --no-install-recommends virtualbox-guest-dkms virtualbox-guest-utils virtualbox-guest-x11
    # other applications
    apt-get install -y firefox git
    su - vagrant -c 'git config --global user.name "Your Name"'
    su - vagrant -c 'git config --global user.email "you@example.com"'
    su - vagrant -c 'mkdir -p ~vagrant/git; cd ~vagrant/git; git clone https://github.com/samrocketman/jervis.git'
    usermod -a -G sudo vagrant
    usermod -a -G docker vagrant
    systemctl enable docker.service
    systemctl start docker.service

    curl -sSfLo /usr/local/bin/docker-compose https://github.com/docker/compose/releases/download/v2.12.2/docker-compose-linux-x86_64
    chmod 755 /usr/local/bin

    # wait for docker startup to complete
    until curl -sSfLo /dev/null --unix-socket /var/run/docker.sock http://localhost/info; do
      sleep 1
    done

    # startup automation
    su - vagrant -c 'code --install-extension ms-vscode-remote.remote-containers'
    su - vagrant -c 'code --install-extension ms-azuretools.vscode-docker'
    mkdir -p ~vagrant/.config/autostart
    echo -e '[Desktop Entry]\nType=Application\nExec=code ~/git/jervis\nHidden=false\nNoDisplay=false\nX-GNOME-Autostart-enabled=true\nName[C]=Open VSCode with Jervis\nName=Open VSCode with Jervis\nComment[C]=\nComment=\n' > ~vagrant/.config/autostart/code.desktop

    mkdir -p ~vagrant/.config/Code/User/
    echo -e '{\n"workbench.startupEditor": "none",\n    "telemetry.telemetryLevel": "off",\n    "dev.containers.dockerComposePath": "/usr/local/bin/docker-compose"\n}\n' > ~vagrant/.config/Code/User/settings.json
    #auto login for vagrant
    mkdir -p /etc/gdm3/
    echo '[daemon]\nAutomaticLoginEnable=True\nAutomaticLogin=vagrant\n[security]\n[xdmcp]\n[chooser]\n[debug]\n' > /etc/gdm3/custom.conf
    # Finalize and reboot to show VSCode automatically
    chown -R vagrant: ~vagrant
    apt-get upgrade -y
    snap refresh
    sudo reboot
  SHELL
end
