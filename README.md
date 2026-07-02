# HTTPSimulatedLiveStreamControl

Start and stop simulated live streams


## Usage
Control the stream through an HTTP request, passing the required variables as query string parameters.  The VOD will play as a live stream, looping until stopped.

1. Add a new stream:
```
curl "http://localhost:8086/streamcontrol?app=live&action=start&stream=testStream&vod=sample.mp4"
```

2. Stop a stream:
```
curl "http://localhost:8086/streamcontrol?app=live&action=stop&stream=testStream"
```

3. Lists streams running (started by this module)
```
curl "http://localhost:8086/streamcontrol?app=live&action=list"
```
Note: currently logs the stream names to the log file


## Config

1. Drop jar into WSE/lib


2. In Port 8086 of VHost.xml, add:

```
<HTTPProvider>
	<BaseClass>com.blankcanvas.video.simlivestreams.HTTPSimulatedLiveStreamControl</BaseClass>
	<RequestFilters>streamcontrol*</RequestFilters>
	<AuthenticationMethod>none</AuthenticationMethod>
	<PasswordEncodingScheme>none</PasswordEncodingScheme>
</HTTPProvider>
```



