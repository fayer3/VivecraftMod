#version 450

layout(binding = 0) uniform sampler2D DiffuseSampler;

layout(location = 0) in vec2 texCoordinates;

layout(location = 0) out vec4 fragColor;

void main(){
    fragColor = texture(DiffuseSampler, texCoordinates.st);
}
