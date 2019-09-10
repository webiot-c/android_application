# android_application
CPRSS 通知受信アプリ。配信サーバー(`webiot-c/raspi_webserver/server.py`)からWebSocketで情報を受け取り、必要に応じてユーザーへの通知を行う。<br>
もともとprivateだったので、publicになる前までのコミットやブランチの切り方は最悪になっている。<br>
なにか提案があるときは、気軽にIssueを投げてほしいです!

## UI
- 受信した内容をメインメニューのリストに表示する。タップすると地図が表示される。<br>
  受信してから有効期限(6～7分を想定)を過ぎると灰色に表示され、ユーザーに削除を促す。
- 日本語と英語の設定が可能。言語はAndroidの言語による。日本語の場合は日本語に、それ以外(中国語なども)は全て英語になる。

## 受信するデータ
WebSocketでデータ受信を行う。<br>

### 送信テキスト
`{送信理由}#{ノード名}#{緯度}#{経度}`<br>

### 内容

|要素|内容|
|:-----|:-----|
|送信理由|送信した理由。`AED-OPEN`・`AED-POLLING`・`AED-CLOSE` より選択する。|
|ノード名|ノードの識別名。|
|緯度|即位した緯度。|
|経度|即位した経度。|

## 改善したい内容
### リファクタリング

- AEDLocationActivityの検討(UIかも)
  - Google Mapアプリに直接アクセスすればいいのでは?

# ファイル構造(Javaファイル)
- `app/java/com/webiot_c/cprss_notifi_recv/`
  - `app/`
    - `service/`
      - **CPRSS_BackgroundAccessService.java**<br>
        サーバーとの通信をバックグラウンドで実施する。
    - **AEDLocationActivity.java**<br>
      AEDの場所を案内する。
    - **BroadcastReceiverManager.java**<br>
      端末の起動時と、アプリのアップデート時にサービスが消えないようにする。
    - **MainActivity.java**<br>
      アプリを起動した際のActivityファイル。
  - `connect/`
    - **CPRSS_WebSocketClient.java**<br>
      サーバーからWebSocketを介して実際に情報を取得し、リスナーに通知する。
    - **CPRSS_WebSocketClientListener.java**<br>
      WebSocketの通信内容の通知を受けるリスナー。
  - `data_struct/`
    - **AEDInformation.java**<br>
      AEDの情報を保存するクラス。
    - **AEDInformationAdapter.java**<br>
      `AEDInformation` クラスの情報を `RecyclerView` 上で表示するためのアダプター。
    - **AEDInformationDatabaseHelper.java**<br>
      端末のデータベースとAEDInformationクラスの情報を関連付けるヘルパー。
  - `utility/`
    - **LocationGetter.java**<br>
      位置情報を取得する。
    - **NotificationUtility.java**<br>
      Android端末に通知を送るためのユーティリティクラス。
  - **DialogActivity.java**<br>
    アプリケーションがアクティブでない状態でダイアログを表示するクラス。
