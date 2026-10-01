#include <Arduino.h>        // Arduinoの基本機能
#include <WiFi.h>           // マイコンの固有番号を安全に取得する
#include <BLEDevice.h>      // BLE機能
#include <BLEServer.h>      // BLEサーバー機能
#include <BLEUtils.h>       // BLEユーティリティ
#include <BLE2902.h>        // BLEの通知機能
#include <Preferences.h>    // メモリ保存機能
#include <Wire.h>           // I2C通信機能
#include <Adafruit_PN532.h> // NFCリーダー用のライブラリ

// BLEのサービスとキャラクタリスティック（通信の窓口）のID（このIDはスマホアプリ側と合わせる）
#define SERVICE_UUID           "9bb2ed40-f4f4-4d66-bb82-a95b64184dd5"
#define CHARACTERISTIC_UUID    "4b43f9a7-d13b-4579-b0a5-af6e4b49a655"

//ピンの定義
#define SDA_PIN D4    // ESP32C3のD4ピンにNFCのSDAを繋ぐかを定義
#define SCL_PIN D6    // ESP32C3のD6ピンにNFCのSCLを繋ぐかを定義
#define PN532_IRQ 2   // ダミー
#define PN532_RESET 3 // ダミー
#define BUTTON_PIN D3 // 物理ボタンを繋ぐピン
#define LIGHT_PIN D1  // ライトを制御するピン

Adafruit_PN532 nfc(PN532_IRQ, PN532_RESET); // ダミーのピンを指定してNFCリーダーのオブジェクト
Preferences preferences;                    // メモリ保存用の箱
BLEServer* pServer = NULL;                  // BLEサーバーのオブジェクトを入れる箱

bool nfcAvailable = false;    // NFCリーダーが基板に繋がっているか
bool deviceConnected = false; // スマホが接続されているか
bool isAuthorized = false;    // 設定モードで認証を通過したかどうか
bool isLightOn = false;       // ライトの点灯状態

// ライトの現在の状態を定義
enum DeviceMode {
  MODE_SETUP,    // 初回セットアップ（BLE:ON, NFC:OFF）
  MODE_NORMAL,   // 通常待機（BLE:OFF, NFC:ON）
  MODE_REGISTER, // 鍵登録（BLE:維持, NFC:ON）
  MODE_SETTING   // 設定（BLE:ON, NFC:OFF）
};

DeviceMode currentMode = MODE_SETUP; // 起動時はセットアップモードを設定

// タイムアウト用の変数（設定モードで使用）
unsigned long stateDStartTime = 0;
const unsigned long TIMEOUT_MS = 180000; // 3分操作がなければ状態Bへ戻る

// BLEの電波をONする
void enableBLE() {
  BLEDevice::getAdvertising()->start();
  Serial.println("BLE通信を有効化");
}

// BLEの電波をOFFする
void disableBLE() {
  BLEDevice::getAdvertising()->stop();
  if (deviceConnected && pServer != NULL) {
    // 接続中の場合は切断する
    pServer->disconnect(0); // 0は全ての接続を切る指定
  }
  Serial.println("BLE通信を無効化");
}

// スマホからの接続・切断を検知するクラス
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("========= 接続 =========");

      if (currentMode == MODE_SETTING) {
        stateDStartTime = millis(); // 接続されたらタイムアウト時間をリセット
      }
    }
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("========= 切断 =========");

      if (currentMode != MODE_NORMAL) {
        enableBLE(); // 通常モード以外なら再び電波を出す
      }
    }
};

// スマホからデータ（文字）が送られてきた時に呼ばれるクラス
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      // 受信したデータをString（文字列）として読み取る
      String rxValue = String(pCharacteristic->getValue().c_str());

      // 前後の見えない空白や改行を消す
      rxValue.trim();

      if (rxValue.length() > 0) {
        Serial.println("受信コマンド: " + rxValue);

        // アプリからのライト制御
        if (rxValue == "LGT_ON") {
          isLightOn = true;       // 状態をONに記憶
          analogWrite(D1, 255); // D1ピンから電気を出してライトをON
          Serial.println("========= 📱アプリ操作：ライトを ON にしました =========");
          return; // 以降の処理はスキップ
        }

        if (rxValue == "LGT_OFF") {
          isLightOn = false;      // 状態をOFFに記憶
          analogWrite(D1, 0);  // D1ピンの電気を止めてライトをOFF
          Serial.println("========= 📱アプリ操作：ライトを OFF にしました =========");
          return; // 以降の処理はスキップ
        }

        // タイムアウト時間を延長
        if (currentMode == MODE_SETTING) {
          stateDStartTime = millis();
        }

        // 初回セットアップ → 鍵登録へ遷移
        if (currentMode == MODE_SETUP) {
          // 秘密の質問の設定 (例: "QUES_1_むぎ")
          if (rxValue.startsWith("QUES_")) {
            String qId = rxValue.substring(5, 6); // "1"だけを切り出す
            String ans = rxValue.substring(7);    // "むぎ"以降をすべて切り出す
            preferences.putString("questionId", qId);
            preferences.putString("answer", ans);
            Serial.println("========= 秘密の質問を設定 (答え: " + ans + ") =========");
          }

          // パスワードの設定 → 完了したら鍵登録へ遷移
          else if (rxValue.startsWith("PASS_")) {
            String password = rxValue.substring(5);
            // ここでハッシュ化処理を入れる
            preferences.putString("password", password);
            Serial.println("========= パスワードを保存 =========");
          }

          // 3つのデータがすべてメモリに存在するかチェック
          String checkPass = preferences.getString("password", "");
          String checkQId  = preferences.getString("questionId", "");
          String checkAns  = preferences.getString("answer", "");

          // すべてのデータが空っぽ("")ではない場合 ＝ 全部保存
          if (checkPass != "" && checkQId != "" && checkAns != "") {
            currentMode = MODE_REGISTER;
            Serial.println("========= 設定完了：鍵登録へ遷移 =========");
          }
        }

        // 設定モード
        else if (currentMode == MODE_SETTING) {
          // 通常のパスワード認証 (例: "LOGIN_1234")
          if (rxValue.startsWith("LOGIN_")) {
            String inputPass = rxValue.substring(6);
            String savedPass = preferences.getString("password", "");

            if (inputPass == savedPass) {
              isAuthorized = true; // 認証OK
              Serial.println("========= 認証成功 =========");
            } else {
              Serial.println("========= パスワードが違います =========");
            }
          }

          // 秘密の質問での認証 (例: "ANS_1_むぎ")
          else if (rxValue.startsWith("ANS_")) {
            String inputQId = rxValue.substring(4, 5); // "1"
            String inputAns = rxValue.substring(6);    // "むぎ"
            
            String savedQId = preferences.getString("questionId", "");
            String savedAns = preferences.getString("answer", "");
            
            if (inputQId == savedQId && inputAns == savedAns) {
              isAuthorized = true; // 認証OK
              Serial.println("========= 認証成功 =========");
            } else {
              Serial.println("========= 答えが違います =========");
            }
          }

          // 認証済みの場合のみ実行可能
          else if (isAuthorized) {
            if (rxValue == "KEY_") {
                currentMode = MODE_REGISTER;
                isAuthorized = false; // 次回のために認証状態をリセット
                Serial.println("========= 鍵登録へ遷移 =========");
            }

            // パスワードの再設定 (例: "REPASS_5678")
            else if (rxValue.startsWith("REPASS_")) {
                String newPass = rxValue.substring(7); // "REPASS_" の後ろの文字を取得
                preferences.putString("password", newPass);
                Serial.println("========= パスワードを再設定しました =========");
            }

            // 個別の鍵の削除 (例: "DELKEY_1234")
            else if (rxValue.startsWith("DELKEY_")) {
                String targetUid = rxValue.substring(7); // "DELKEY_" の後ろのUIDを取得
                preferences.remove(targetUid.c_str()); // メモリから指定した名前のデータを消去
                Serial.println("========= 鍵 [" + targetUid + "] を削除しました =========");
            }

            // 設定終了
            else if (rxValue == "END_") {
              currentMode = MODE_NORMAL;
              disableBLE();
              isAuthorized = false; // 次回のために認証状態をリセット
              Serial.println("========= 通常待機へ遷移 =========");
            }
          }else { 
            // 認証していないのにコマンドを送ってきた場合
            Serial.println("========= 認証してください =========");
          }
        }
      }
    }
};

void setup() {
  Serial.begin(115200); // シリアルモニタの通信速度
  delay(2000);          // シリアルモニタの起動待ち

  pinMode(BUTTON_PIN, INPUT_PULLUP); // ボタンのピン設定（内部プルアップ）
  pinMode(LIGHT_PIN, OUTPUT);               // LIGHT_PINを電気を出力するピンに設定
  analogWrite(LIGHT_PIN, 0);             // 起動時はライトをOFF

  // 保存領域の初期化（"lightouch"という名前のデータ領域を開く）
  preferences.begin("lighTouch", false); 

  // // テスト用：起動時に記憶を全消去する
  // preferences.clear(); 

  // NFCの初期化
  Wire.begin(SDA_PIN, SCL_PIN);
  nfc.begin();                                     // NFCリーダーとの通信を開始
  uint32_t versiondata = nfc.getFirmwareVersion(); // NFCリーダーのバージョン取得
  
  if (!versiondata) { 
    // NFCが繋がっていない場合
    Serial.println("========= NFCリーダーが見つかりません =========");
    nfcAvailable = false; // エラーで止めず、NFCなしモードとしてフラグを折るだけにする
  }else { 
    // NFCが繋がっている場合      
    nfc.SAMConfig();      // NFCを読み取りモードに設定
    nfcAvailable = true;  // NFCありのフラグを立てる
  }

  // // BLEデバイスの初期化（スマホに表示される名前）
  // BLEDevice::init("LighTouch");

  // ESP32の固有番号を取得（例: "AA:BB:CC:11:22:33"）
  String mac = WiFi.macAddress(); 
  
  // 後ろの4文字を取得して繋げる
  String shortMac = mac.substring(12, 14) + mac.substring(15, 17);
  
  // 名前を合体させる（例: "LighTouch_2233"）
  String deviceName = "LighTouch_" + shortMac;
  
  // 合体した名前でBLEを起動
  BLEDevice::init(deviceName.c_str());

  // BLEサーバーの作成
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // サービスの作成
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // 読み取り・書き込み権限を付与
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );

  // データを受信したときの処理をセット
  pCharacteristic->setCallbacks(new MyCallbacks());

  // サービスの開始
  pService->start();

  // 起動時の状態判定
  String savedPass = preferences.getString("password", "");
  if (savedPass == "") {
    currentMode = MODE_SETUP;
    Serial.println("========= 起動完了：初回セットアップ =========");
    enableBLE();
  }else {
    // パスワードが保存されている場合は通常待機に入る
    currentMode = MODE_NORMAL;
    Serial.println("========= 起動完了：通常待機 =========");
    // disableBLE(); // 状態BはBLEを無効化
    enableBLE(); // スマホから繋げるようにBLEをON

    // // テスト用に強制的に設定モードにする
    // currentMode = MODE_SETTING; // 強制的に設定モードにする
    // Serial.println("========= 起動完了：【テスト用】設定待機 =========");
    // enableBLE(); // スマホから繋げるようにBLEをON
  }
}

void loop() {
  // 物理ボタンの監視（通常待機 → 設定のトリガー）
  if (currentMode == MODE_NORMAL) {
    if (digitalRead(BUTTON_PIN) == LOW) { // ボタンが押されたら（GNDに落ちたら）
      delay(50); // チャタリング防止

      if (digitalRead(BUTTON_PIN) == LOW) {
        currentMode = MODE_SETTING;
        stateDStartTime = millis();
        isAuthorized = false; // 設定モードに入るときは認証状態をリセット
        enableBLE();
        Serial.println("========= ボタン検知：設定へ遷移 =========");
        while(digitalRead(BUTTON_PIN) == LOW); // ボタンを離すまで待機
      }
    }
  }

  // 設定のタイムアウト監視（一定時間操作なしで通常待機へ）
  if (currentMode == MODE_SETTING) {
    if (millis() - stateDStartTime > TIMEOUT_MS) {
      currentMode = MODE_NORMAL;
      disableBLE();
      Serial.println("========= タイムアウト：通常待機へ遷移 =========");
    }
  }

  // NFCの監視（通常待機 および 鍵登録）
  if (nfcAvailable && (currentMode == MODE_NORMAL || currentMode == MODE_REGISTER)) {
    uint8_t success;
    uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };
    uint8_t uidLength;

    success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 50);

    if (success) {
      String uidString = "";
      for (uint8_t i = 0; i < uidLength; i++) {
        if (uid[i] <= 0x0F) uidString += "0";
        uidString += String(uid[i], HEX);
      }

      uidString.toUpperCase();

      if (currentMode == MODE_REGISTER) {
        // 鍵登録処理
        preferences.putString(uidString.c_str(), "registered");
        Serial.println("========= 鍵を登録: " + uidString + " =========");
        
        // 登録完了と同時に状態Bへ自動遷移
        currentMode = MODE_NORMAL;
        disableBLE();
        Serial.println("========= 通常待機へ遷移 =========");
      } 
      else if (currentMode == MODE_NORMAL) {
        // 認証処理
        String isRegistered = preferences.getString(uidString.c_str(), "");

        if (isRegistered == "registered") {
          isLightOn = !isLightOn; // ライトの状態を反転
          
          // 認証成功時
          if (isLightOn) {
            analogWrite(LIGHT_PIN, 255); // ライトON
            Serial.println("========= 認証成功：ライトを ON にしました =========");
          } else {
            analogWrite(LIGHT_PIN, 0);  // ライトOFF
            Serial.println("========= 認証成功：ライトを OFF にしました =========");
          }
        } else {
          Serial.println("========= 未登録のタグ =========");
        }
      }

      delay(2000); // 連続読み取り防止
    }
  }
}