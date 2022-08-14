#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint datecs_printer.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'datecs_printer'
  s.version          = '0.0.6'
  s.summary          = 'Datecs printer ios plugin'
  s.description      = <<-DESC
  Datecs printer ios plugin.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.platform = :ios, '9.0'
  s.static_framework = true

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 arm64' }
  s.swift_version = '5.0'
  
  s.preserve_paths = 'PrinterSDK.xcframework/**/*'
  s.xcconfig = { 'OTHER_LDFLAGS' => '-framework PrinterSDK' }
  s.vendored_frameworks = 'PrinterSDK.xcframework'
end
