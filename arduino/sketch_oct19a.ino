#include <Stepper.h>
#include <SoftwareSerial.h>// 블루투스 시리얼 통신 라이브러리 추가

SoftwareSerial BTSerial(8, 9);  // 블루투스 설정 BTSerial(Tx, Rx) (보내는 핀, 받는 핀)
  
const int stepsPerRevolution = 400; //회전수 200/한바퀴
Stepper myStepper(stepsPerRevolution, 3,4,6,7); //8,10,9,11
int ENA=2;
int ENB=5;
int gas1 = 300;
//int gas2 = 20;

boolean window = false; //false -> 닫혀있는상태
//true -> 열려있는상태

const int gasPin = A0; // 가스
const int rainPin = A1;

void setup(){
  Serial.begin(9600);
  BTSerial.begin(9600); 
  myStepper.setSpeed(30);
  pinMode(ENA,OUTPUT); //
  pinMode(ENB,OUTPUT);//
  pinMode(gasPin, INPUT);
  pinMode(rainPin, INPUT);
  digitalWrite(ENA,HIGH);//
  digitalWrite(ENB,HIGH);//
  }

void loop(){
  BTSerial.println(0xff);
  delay(1000);
  while(BTSerial.available() > 0)
  {
      char response = BTSerial.read();
      Serial.write(response);
  }
//  int rain_value = analogRead(rainPin);
//  int gas_value = analogRead(gasPin);
//  
//  Serial.print("Gas : ");
//  Serial.println(gas_value);
//  Serial.println();
//  Serial.print("Rain : ");
//  Serial.println(rain_value);
//  Serial.println();
//  Serial.print("Window : ");
//  if(window==false) Serial.println("Close");
//  else if(window==true) Serial.println("Open");
//  Serial.println("=============================================== ");
//  Serial.println();
//  
//  if(gas_value>=gas1 && window == false){ // 창문열기 조건(창문이 닫혀있고 가스가 기준 이상 감지될때)
//    sensor_open();
//  }
//  else if(gas_value<gas1 && window == true && rain_value<=400){ //창문닫기 조건(빗물이 감지되고 가스가 기준 이하일때)
//    sensor_close();
//  }
//  delay(5000);
}

void sensor_open(){
  Serial.println("------------");
  Serial.println("****OPEN****");
  Serial.println("------------");
  myStepper.step(stepsPerRevolution);
  //Serial.println("stop");
  window = true;
  
  Serial.print("Window Status : ");
  Serial.println(window);
  Serial.println("=============================================== ");
  Serial.println();
}

void sensor_close(){
  Serial.println("------------");
  Serial.println("****CLOSE****");
  Serial.println("------------");
  myStepper.step(-stepsPerRevolution);
  window = false;
  
  Serial.print("Window Status : ");
  Serial.println(window);
  Serial.println("=============================================== ");
  Serial.println();
}
