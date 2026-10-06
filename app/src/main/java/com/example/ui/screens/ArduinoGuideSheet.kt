package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val HYDROSENSE_MEGA_CODE = """#include <Arduino.h>

const byte TRIG_PIN=9,ECHO_PIN=10,BUZZER_PIN=8,RELAY_PIN=7;
const byte RELAY_ON=LOW, RELAY_OFF=HIGH;
const float EMPTY_DISTANCE=14.00,FULL_DISTANCE=2.42,SAFETY_LEVEL=95.0;
bool motorRunning=false,sensorValid=false;
float targetLevel=80.0,distanceCM=0.0,waterLevel=0.0;
unsigned int sensorFailCount=0;
const unsigned int SENSOR_FAIL_LIMIT=50;
const unsigned long SENSOR_INTERVAL=100,BLUETOOTH_INTERVAL=1000;
unsigned long lastSensorRead=0,lastBluetoothSend=0,lastHeartbeat=0,heartbeatCount=0;
const byte FILTER_SIZE=5;
float distanceBuffer[FILTER_SIZE];
byte bufferIndex=0,bufferCount=0,targetConfirmCount=0,safetyConfirmCount=0;
const byte REQUIRED_CONFIRMATIONS=3;
char commandBuffer[40]; byte commandIndex=0;

enum BuzzerMode{BUZZER_OFF,BUZZER_SLOW,BUZZER_FAST,BUZZER_CONTINUOUS};
BuzzerMode buzzerMode=BUZZER_OFF;
bool buzzerState=false,targetBeepActive=false;
unsigned long buzzerTimer=0,targetBeepStart=0;
const unsigned long TARGET_BEEP_TIME=800;

void setup(){
  pinMode(TRIG_PIN,OUTPUT); pinMode(ECHO_PIN,INPUT);
  pinMode(BUZZER_PIN,OUTPUT); noTone(BUZZER_PIN);
  pinMode(RELAY_PIN,OUTPUT); digitalWrite(RELAY_PIN,RELAY_OFF);
  Serial.begin(9600); Serial1.begin(9600); delay(300);
  Serial.println("==========================================");
  Serial.println("HYDROSENSE - MEGA 2560 FINAL");
  Serial.println("SYSTEM READY");
  Serial.println("HC-05: SERIAL1 9600 | TRIG:D9 | ECHO:D10 | BUZZER:D8 | RELAY:D7");
  Serial.print("EMPTY: ");Serial.print(EMPTY_DISTANCE,2);Serial.println(" cm");
  Serial.print("FULL: ");Serial.print(FULL_DISTANCE,2);Serial.println(" cm");
  Serial.print("TARGET: ");Serial.print(targetLevel,1);Serial.println("%");
  Serial.print("SAFETY: ");Serial.print(SAFETY_LEVEL,1);Serial.println("%");
  Serial.println("AUTO START: DISABLED");
  Serial1.println("HYDROSENSE:MEGA_READY");
}

void loop(){
  readBluetooth();
  if(millis()-lastSensorRead>=SENSOR_INTERVAL){lastSensorRead=millis();readSensor();}
  if(motorRunning && sensorFailCount>=SENSOR_FAIL_LIMIT){
    stopMotor(); Serial.println("SENSOR FAILSAFE - MOTOR STOP");
    Serial1.println("MOTOR_OFF:SENSOR_TIMEOUT");
  }
  updateBuzzer(); updateTargetBeep();
  if(millis()-lastHeartbeat>=2000){
    lastHeartbeat=millis(); heartbeatCount++;
    Serial.print("HEARTBEAT | COUNT=");Serial.print(heartbeatCount);
    Serial.print(" | LEVEL=");Serial.print(waterLevel,1);
    Serial.print("% | DIST=");Serial.print(distanceCM,2);
    Serial.print("cm | MOTOR=");Serial.print(motorRunning?"ON":"OFF");
    Serial.print(" | SENSOR=");Serial.print(sensorValid?"OK":"ERROR");
    Serial.print(" | FAIL=");Serial.println(sensorFailCount);
  }
}

void readSensor(){
  digitalWrite(TRIG_PIN,LOW);delayMicroseconds(2);
  digitalWrite(TRIG_PIN,HIGH);delayMicroseconds(10);digitalWrite(TRIG_PIN,LOW);
  unsigned long duration=pulseIn(ECHO_PIN,HIGH,30000);
  if(duration==0){sensorValid=false;sensorFailCount++;return;}
  float d=duration*0.0343/2.0;
  if(d<1.0||d>400.0){sensorValid=false;sensorFailCount++;return;}
  sensorValid=true;sensorFailCount=0;
  distanceBuffer[bufferIndex]=d; if(++bufferIndex>=FILTER_SIZE)bufferIndex=0;
  if(bufferCount<FILTER_SIZE)bufferCount++;
  float total=0;for(byte i=0;i<bufferCount;i++)total+=distanceBuffer[i];
  distanceCM=total/bufferCount; calculateLevel(); checkMotor(); sendBluetoothData();
}

void calculateLevel(){
  float level=((EMPTY_DISTANCE-distanceCM)/(EMPTY_DISTANCE-FULL_DISTANCE))*100.0;
  if(level<0)level=0;if(level>100)level=100;
  static float smoothLevel=0;
  smoothLevel=smoothLevel*0.75+level*0.25;waterLevel=smoothLevel;
  if(waterLevel<0)waterLevel=0;if(waterLevel>100)waterLevel=100;
}

void checkMotor(){
  if(!motorRunning)return;
  if(waterLevel>=SAFETY_LEVEL)safetyConfirmCount++;else safetyConfirmCount=0;
  if(safetyConfirmCount>=REQUIRED_CONFIRMATIONS){
    safetyConfirmCount=0;targetConfirmCount=0;stopMotor();
    Serial.println("MOTOR STOP: SAFETY 95%");Serial1.println("MOTOR_OFF:SAFETY_LIMIT");startTargetBeep();return;
  }
  if(waterLevel>=targetLevel)targetConfirmCount++;else targetConfirmCount=0;
  if(targetConfirmCount>=REQUIRED_CONFIRMATIONS){
    targetConfirmCount=0;safetyConfirmCount=0;stopMotor();
    Serial.println("MOTOR STOP: TARGET REACHED");Serial1.println("MOTOR_OFF:TARGET_REACHED");startTargetBeep();
  }
}

void startMotor(){
  if(!sensorValid){Serial1.println("ACK:MOTOR_ON:ERROR");return;}
  if(waterLevel>=SAFETY_LEVEL){Serial1.println("ACK:MOTOR_ON:ERROR");return;}
  if(waterLevel>=targetLevel){Serial1.println("ACK:MOTOR_ON:ERROR");return;}
  targetConfirmCount=0;safetyConfirmCount=0;sensorFailCount=0;
  digitalWrite(RELAY_PIN,RELAY_ON);motorRunning=true;
  Serial.println("MOTOR ON");Serial1.println("ACK:MOTOR_ON:OK");
}

void stopMotor(){
  digitalWrite(RELAY_PIN,RELAY_OFF);motorRunning=false;
  targetConfirmCount=0;safetyConfirmCount=0;
  Serial.println("MOTOR OFF");Serial1.println("ACK:MOTOR_OFF:OK");
}

void readBluetooth(){
  while(Serial1.available()){
    char c=Serial1.read();
    if(c=='\n'||c=='\r'){
      if(commandIndex>0){commandBuffer[commandIndex]='\0';processCommand(commandBuffer);commandIndex=0;}
    }else if(commandIndex<sizeof(commandBuffer)-1)commandBuffer[commandIndex++]=c;
    else commandIndex=0;
  }
}

void processCommand(const char* cmd){
  Serial.print("BT COMMAND: ");Serial.println(cmd);
  if(strcmp(cmd,"MOTOR_ON")==0){startMotor();return;}
  if(strcmp(cmd,"MOTOR_OFF")==0){stopMotor();return;}
  if(strcmp(cmd,"PING")==0){Serial1.println("PONG");return;}
  if(strcmp(cmd,"STATUS")==0){sendStatus();return;}
  if(strncmp(cmd,"TARGET:",7)==0){
    float t=atof(cmd+7);
    if(t>=1.0&&t<=95.0){
      targetLevel=t;targetConfirmCount=0;safetyConfirmCount=0;Serial1.println("ACK:TARGET:OK");
      if(motorRunning&&waterLevel>=targetLevel){stopMotor();Serial1.println("MOTOR_OFF:TARGET_REACHED");startTargetBeep();}
    }else Serial1.println("ACK:TARGET:ERROR");
    return;
  }
  if(strcmp(cmd,"CAL_EMPTY")==0){Serial1.println("ACK:CAL_EMPTY:OK");return;}
  if(strcmp(cmd,"CAL_FULL")==0){Serial1.println("ACK:CAL_FULL:OK");return;}
  Serial1.println("ACK:UNKNOWN");
}

void sendBluetoothData(){
  if(millis()-lastBluetoothSend<BLUETOOTH_INTERVAL)return;
  lastBluetoothSend=millis();
  Serial1.print("DISTANCE:");Serial1.print(distanceCM,2);
  Serial1.print(",LEVEL:");Serial1.print(waterLevel,1);
  Serial1.print(",MOTOR:");Serial1.print(motorRunning?"ON":"OFF");
  Serial1.print(",TARGET:");Serial1.println(targetLevel,1);
}

void sendStatus(){
  Serial1.print("STATUS:EMPTY:");Serial1.print(EMPTY_DISTANCE,2);
  Serial1.print(",FULL:");Serial1.print(FULL_DISTANCE,2);
  Serial1.print(",LEVEL:");Serial1.print(waterLevel,1);
  Serial1.print(",TARGET:");Serial1.print(targetLevel,1);
  Serial1.print(",MOTOR:");Serial1.println(motorRunning?"ON":"OFF");
}

void startTargetBeep(){targetBeepActive=true;targetBeepStart=millis();tone(BUZZER_PIN,2000);}
void updateTargetBeep(){if(targetBeepActive&&millis()-targetBeepStart>=TARGET_BEEP_TIME){noTone(BUZZER_PIN);targetBeepActive=false;}}

void updateBuzzer(){
  if(targetBeepActive)return;
  unsigned long now=millis();
  if(waterLevel<90.0){
    if(buzzerMode!=BUZZER_OFF){noTone(BUZZER_PIN);buzzerMode=BUZZER_OFF;buzzerState=false;}return;
  }
  if(waterLevel>=98.90){
    if(buzzerMode!=BUZZER_CONTINUOUS){buzzerMode=BUZZER_CONTINUOUS;tone(BUZZER_PIN,2000);}return;
  }
  if(waterLevel>95.0){
    if(buzzerMode!=BUZZER_FAST){buzzerMode=BUZZER_FAST;buzzerState=false;buzzerTimer=now;noTone(BUZZER_PIN);}
    if(now-buzzerTimer>=400){buzzerTimer=now;buzzerState=!buzzerState;if(buzzerState)tone(BUZZER_PIN,2000);else noTone(BUZZER_PIN);}
    return;
  }
  if(waterLevel>=90.0){
    if(buzzerMode!=BUZZER_SLOW){buzzerMode=BUZZER_SLOW;buzzerState=false;buzzerTimer=now;noTone(BUZZER_PIN);}
    if(now-buzzerTimer>=1000){buzzerTimer=now;buzzerState=!buzzerState;if(buzzerState)tone(BUZZER_PIN,2000);else noTone(BUZZER_PIN);}
  }
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArduinoGuideSheet(
  sheetState: SheetState,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(scrollState)
        .testTag("arduino_guide_sheet")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "HydroSense Mega 2560 Code",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Matched Bluetooth Protocol & Pinout",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Copy Button
      Button(
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("HydroSense Arduino Code", HYDROSENSE_MEGA_CODE)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "Arduino code copied to clipboard!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Copy HydroSense Arduino Code", fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Pin Connections Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(text = "Arduino Mega 2560 Pinout & Protocol:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          Text(text = "• HC-05 TX  -> Mega Pin 19 (RX1)", fontSize = 12.sp)
          Text(text = "• HC-05 RX  -> Mega Pin 18 (TX1)", fontSize = 12.sp)
          Text(text = "• HC-05 Baud Rate -> Serial1 9600", fontSize = 12.sp)
          Text(text = "• Ultrasonic Trig -> Pin D9, Echo -> Pin D10", fontSize = 12.sp)
          Text(text = "• Relay (Motor)   -> Pin D7 (Active LOW = ON)", fontSize = 12.sp)
          Text(text = "• Buzzer Alert    -> Pin D8", fontSize = 12.sp)
          Text(text = "• Empty Distance  -> 14.00 cm", fontSize = 12.sp)
          Text(text = "• Full Distance   -> 2.42 cm", fontSize = 12.sp)
          Text(text = "• Safety Limit    -> 95.0% auto shutoff", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Code View
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A)
      ) {
        Text(
          text = HYDROSENSE_MEGA_CODE,
          modifier = Modifier.padding(14.dp),
          color = Color(0xFF94A3B8),
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          lineHeight = 16.sp
        )
      }
    }
  }
}
