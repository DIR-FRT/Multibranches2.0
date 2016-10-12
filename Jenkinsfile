node {
   // Mark the code checkout 'stage'....
   stage 'Checkout'
   env.PATH = "${tool 'Maven 3'}/bin:${env.PATH}"
   checkout scm
   sh 'mvn clean package'
}
