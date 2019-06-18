# FrameCamera

It be able to Use camera2 API easily.

## Description

Wrapper library of camera2 api to use easily.

## Feature(will add soon...)

make picture with frame and decoration.
support switching camera direction for front and back.
Touch focus, auto focus, continuous shooting.

## usage

it simply call static method as single line. use activity instance for argument
```groovy
	CameraActivity.startCameraActivity(this)

```

how to get result picture.
simply call method as single line on activity result!
```groovy
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

	val bmp = CameraActivity.getResult(requestCode, resultCode, data)
    }
```