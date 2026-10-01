#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <WiFi.h>

// Nordic UART Service UUIDs
#define NUS_SERVICE_UUID    "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define NUS_RX_CHAR_UUID    "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"  // Phone -> XIAO
#define NUS_TX_CHAR_UUID    "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"  // XIAO  -> Phone

#define DEVICE_NAME         "XIAO_ESP32C3"
#define LED_PIN             10  // XIAO ESP32C3 内蔵LED

BLEServer*          pServer           = nullptr;
BLECharacteristic*  pTxCharacteristic = nullptr;
bool                deviceConnected   = false;

// ===== 接続状態コールバック =====
class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    deviceConnected = true;
    Serial.println("[BLE] デバイス接続");
  }
  void onDisconnect(BLEServer* pServer) override {
    deviceConnected = false;
    Serial.println("[BLE] デバイス切断 — アドバタイズ再開");
    BLEDevice::startAdvertising();
  }
};

// ===== ヘルパー: コロン区切りでトークン取得 =====
String getToken(const String& str, int index) {
  int start = 0, count = 0;
  for (int i = 0; i <= (int)str.length(); i++) {
    if (i == (int)str.length() || str[i] == ':') {
      if (count == index) return str.substring(start, i);
      start = i + 1;
      count++;
    }
  }
  return "";
}

// ===== RX 受信コールバック（Android → XIAO）=====
class RxCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    std::string rxValue = pCharacteristic->getValue();
    if (rxValue.empty()) return;

    String cmd = String(rxValue.c_str());
    Serial.print("[RX] 受信: ");
    Serial.println(cmd);

    // --- LED 制御 ---
    if (cmd == "LED_ON") {
      digitalWrite(LED_PIN, LOW);   // XIAO は LOW で点灯
      sendResponse("LED_ON:OK");

    } else if (cmd == "LED_OFF") {
      digitalWrite(LED_PIN, HIGH);
      sendResponse("LED_OFF:OK");

    // --- GPIO 汎用制御: PIN_HIGH:<pin> / PIN_LOW:<pin> ---
    } else if (cmd.startsWith("PIN_HIGH:")) {
      int pin = getToken(cmd, 1).toInt();
      pinMode(pin, OUTPUT);
      digitalWrite(pin, HIGH);
      sendResponse("PIN_HIGH:" + String(pin) + ":OK");

    } else if (cmd.startsWith("PIN_LOW:")) {
      int pin = getToken(cmd, 1).toInt();
      pinMode(pin, OUTPUT);
      digitalWrite(pin, LOW);
      sendResponse("PIN_LOW:" + String(pin) + ":OK");

    // --- GPIO 読み取り: PIN_READ:<pin> ---
    } else if (cmd.startsWith("PIN_READ:")) {
      int pin = getToken(cmd, 1).toInt();
      pinMode(pin, INPUT);
      int val = digitalRead(pin);
      sendResponse("PIN_READ:" + String(pin) + ":" + String(val));

    // --- アナログ読み取り: ADC_READ:<pin> ---
    } else if (cmd.startsWith("ADC_READ:")) {
      int pin = getToken(cmd, 1).toInt();
      int val = analogRead(pin);
      sendResponse("ADC_READ:" + String(pin) + ":" + String(val));

    // --- Wi-Fi 接続: WIFI_CONNECT:<ssid>:<password> ---
    } else if (cmd.startsWith("WIFI_CONNECT:")) {
      String ssid = getToken(cmd, 1);
      String pass = getToken(cmd, 2);
      WiFi.begin(ssid.c_str(), pass.c_str());
      sendResponse("WIFI_CONNECT:TRYING");
      int retry = 0;
      while (WiFi.status() != WL_CONNECTED && retry < 20) {
        delay(500);
        retry++;
      }
      if (WiFi.status() == WL_CONNECTED) {
        sendResponse("WIFI_CONNECT:OK:" + WiFi.localIP().toString());
      } else {
        sendResponse("WIFI_CONNECT:FAIL");
      }

    // --- Wi-Fi 切断 ---
    } else if (cmd == "WIFI_DISCONNECT") {
      WiFi.disconnect();
      sendResponse("WIFI_DISCONNECT:OK");

    // --- Wi-Fi 状態確認 ---
    } else if (cmd == "WIFI_STATUS") {
      if (WiFi.status() == WL_CONNECTED) {
        sendResponse("WIFI_STATUS:CONNECTED:" + WiFi.localIP().toString()
                     + ":" + WiFi.SSID()
                     + ":RSSI:" + String(WiFi.RSSI()));
      } else {
        sendResponse("WIFI_STATUS:DISCONNECTED");
      }

    // --- デバイス情報 ---
    } else if (cmd == "STATUS") {
      String info = "STATUS:";
      info += "UPTIME:" + String(millis() / 1000) + "s";
      info += ":WIFI:" + String(WiFi.status() == WL_CONNECTED ? "ON" : "OFF");
      info += ":FREE_HEAP:" + String(ESP.getFreeHeap());
      sendResponse(info);

    // --- デバイスリセット ---
    } else if (cmd == "RESET") {
      sendResponse("RESET:OK");
      delay(500);
      ESP.restart();

    } else {
      sendResponse("UNKNOWN:" + cmd);
    }
  }

  void sendResponse(String msg) {
    if (!deviceConnected || pTxCharacteristic == nullptr) return;
    pTxCharacteristic->setValue(msg.c_str());
    pTxCharacteristic->notify();
    Serial.print("[TX] 送信: ");
    Serial.println(msg);
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);  // 消灯

  WiFi.mode(WIFI_STA);
  WiFi.disconnect();

  // BLE 初期化
  BLEDevice::init(DEVICE_NAME);
  BLEDevice::setMTU(517);

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  // NUS サービス作成
  BLEService* pService = pServer->createService(NUS_SERVICE_UUID);

  // TX キャラクタリスティック（XIAO → Android、Notify）
  pTxCharacteristic = pService->createCharacteristic(
    NUS_TX_CHAR_UUID,
    BLECharacteristic::PROPERTY_NOTIFY
  );
  pTxCharacteristic->addDescriptor(new BLE2902());

  // RX キャラクタリスティック（Android → XIAO、Write）
  BLECharacteristic* pRxCharacteristic = pService->createCharacteristic(
    NUS_RX_CHAR_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
  );
  pRxCharacteristic->setCallbacks(new RxCallbacks());

  pService->start();

  // アドバタイズ設定
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(NUS_SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  BLEDevice::startAdvertising();

  Serial.println("[BLE] アドバタイズ開始: " DEVICE_NAME);
}

void loop() {
  delay(1000);
}
