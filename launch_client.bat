@echo off
title PeerDesk Client
cd /d "%~dp0"
java -cp target\classes com.mycompany.peerdesk.PeerDesk --client
