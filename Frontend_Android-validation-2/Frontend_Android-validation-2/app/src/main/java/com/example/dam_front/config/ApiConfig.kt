package com.example.dam_front.config

object ApiConfig {
    // Pour l'émulateur Android, utilisez 10.0.2.2 au lieu de localhost
    // Pour un appareil physique, utilisez l'IP de votre ordinateur (ex: 192.168.1.100)
    // Changez cette valeur selon votre environnement

    const val BASE_URL = "https://sprotifkids.onrender.com/" // Émulateur Android (localhost du PC hôte)
    //const val BASE_URL = "http://172.18.7.116:3000/" // Appareil physique (remplacez par votre IP)
    // const val BASE_URL = "http://10.0.2.16:3000/" // Si 10.0.2.2 ne fonctionne pas, essayez cette IP

    // URL pour Socket.IO (même base que l'API REST)
    const val SOCKET_URL = "https://sprotifkids.onrender.com/"

    // AI Configuration
    const val GEMINI_API_KEY = "AIzaSyCuXJbGT4NrP-MPa51EOR14DNc7DKrx1A8"
}


