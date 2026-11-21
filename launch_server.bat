@echo off
title PeerDesk Server
cd /d "%~dp0"
java -cp target\classes com.mycompany.peerdesk.PeerDesk --server
pause
