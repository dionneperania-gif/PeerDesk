@echo off
title PeerDesk 3-Client Demo Launcher
cd /d "%~dp0"

echo Starting PeerDesk Server...
start "" javaw -cp target\classes com.mycompany.peerdesk.PeerDesk --server

timeout /t 2 /nobreak >nul

echo Launching Client 1 (Alice)...
start "" javaw -cp target\classes com.mycompany.peerdesk.PeerDesk --client

echo Launching Client 2 (Bob)...
start "" javaw -cp target\classes com.mycompany.peerdesk.PeerDesk --client

echo Launching Client 3 (Charlie)...
start "" javaw -cp target\classes com.mycompany.peerdesk.PeerDesk --client

echo All 4 windows launched successfully!
