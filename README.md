# Mac InputSource control Demo.

## 概要
JNAを使ってInputSourceを切り替える実証テスト。  
https://sjhannah.com/blog/2012/10/29/speaking-cocoa-from-java/  

TISSelectInputSource法ではメニューバーの入力ソース表示が更新されない場合があり、NSTextInputContext法を使う。  

InputSource切り替えはmain threadからdispatch_syncしなければならない。 
main threadからのCallback内でJNAでInputSource切り替える処理を行う。
Swing threadから呼び出すとmain threadと競合するので、別スレッドから呼ばなければいけない。  

ライセンスはGPL v2とします。

Masudana Ika
