
out vec4 outputColor;

uniform vec4 input_color;

uniform mat4 view_matrix;

// Light properties
uniform vec3 lightPos;
uniform vec3 lightIntensity;
uniform vec3 ambientIntensity;

// Material properties
uniform vec3 ambientCoeff;
uniform vec3 diffuseCoeff;
uniform vec3 specularCoeff;
uniform float phongExp;

uniform sampler2D tex;

in vec4 viewPosition;
in vec3 m;

in vec2 texCoordFrag;

// torch properties
uniform float torchEnabled;
uniform float cutoff;
uniform float spotExp;
uniform vec3 cameraPos;
uniform vec3 torchDiffuseCoeff;
uniform vec3 torchSpecularCoeff;
uniform vec3 torchLightDirection;

// distance attenuation properties
uniform float constant;
uniform float linear;
uniform float quadratic;

void main()
{
    // Compute the s, v and r vectors
    vec3 s = normalize(view_matrix*vec4(lightPos,0)).xyz;
    vec3 v = normalize(-viewPosition.xyz);
    vec3 r = normalize(reflect(-s,m));

    vec3 ambient = ambientIntensity*ambientCoeff;
    vec3 diffuse = max(lightIntensity*diffuseCoeff*dot(m,s), 0.0);
    vec3 specular;

    // Only show specular reflections for the front face
    if (dot(m,s) > 0)
        specular = max(lightIntensity*specularCoeff*pow(dot(r,v),phongExp), 0.0);
    else
        specular = vec3(0);

    vec4 ambientAndDiffuse = vec4(ambient + diffuse, 1);

    if (torchEnabled == 1) {
        // Spot direction
        vec3 spotS = normalize(view_matrix*vec4(cameraPos, 1) - viewPosition).xyz;
        // theta - angle between Spot Direction and Light direction
        float theta = dot(-spotS, torchLightDirection);
        if(theta > cos(radians(cutoff))){
            // calculate distance attenuation
            float distance =  length(vec4(cameraPos, 1) - viewPosition);
            float attenuation = 1.0/(constant + (linear * distance) + (quadratic * distance * distance));

            diffuse = max(lightIntensity*torchDiffuseCoeff*theta, 0.0);
            diffuse *= attenuation;
            ambientAndDiffuse = vec4(ambient + diffuse, 1);
            specular = max(lightIntensity*torchSpecularCoeff*pow(theta, spotExp), 0.0);
            specular *= attenuation;
        }
    }
    outputColor = ambientAndDiffuse*input_color*texture(tex, texCoordFrag) + vec4(specular, 1);
}
