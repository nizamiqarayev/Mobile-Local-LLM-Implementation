# Native iOS frameworks

`llama.xcframework` contains only the iPhone device and iPhone simulator slices
extracted from llama.cpp's official Apple build. Regenerate it after updating
the vendored llama.cpp revision:

```shell
cd third_party/llama.cpp
./build-xcframework.sh

cd ../..
xcodebuild -create-xcframework \
  -framework third_party/llama.cpp/build-ios-sim/framework/llama.framework \
  -framework third_party/llama.cpp/build-ios-device/framework/llama.framework \
  -output ios/Frameworks/llama.xcframework
```
