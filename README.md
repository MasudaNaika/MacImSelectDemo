# Mac InputSource control Demo.

## 概要
　JNAを使ってInputSourceを切り替える実証テスト。  
https://sjhannah.com/blog/2012/10/29/speaking-cocoa-from-java/  

InputSource切り替えはmain threadからdispatch_syncしなければならない。  
Swing threadから呼び出すとmain threadと競合するので、別スレッドから呼ばなければいけない。  

ライセンスはGPL v2とします。

Masudana Ika
