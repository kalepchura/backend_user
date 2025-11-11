pipeline {
    agent any
    tools {
        maven 'Maven'    // Nombre de Maven configurado en Jenkins
        jdk 'Java'       // Nombre de JDK configurado en Jenkins
    }
    stages {
        stage('Checkout') {
            steps {
                echo '📥 Obteniendo código...'
                checkout scm
            }
        }
        stage('Build') {
            steps {
                echo '🔨 Compilando...'
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                echo '🧪 Ejecutando tests...'
                sh 'mvn test'
            }
        }
        stage('Package') {
            steps {
                echo '📦 Empaquetando...'
                sh 'mvn package -DskipTests'
            }
        }
    }
}
