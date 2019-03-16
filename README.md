# wechatSkip
微信跳一跳辅助

项目使用了OpenCV进行图像的处理与分析。所以需要将/lib/opencv/opencv-400.jar添加到library，
启动项目时需要加上启动参数：-Djava.library.path=C:/Users/zhong/IdeaProjects/gelin_game/opencv/x64，根据系统选择x64或者x86。

还使用了ADB进行图片截屏，上传，模拟点击，所以需要配置ADB工具并添加环境变量。

手机需要开启USB调试，部分手机可能还需要开启模拟点击。
