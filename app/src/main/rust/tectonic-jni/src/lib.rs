use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::path::PathBuf;
use tectonic::driver::{ProcessingSessionBuilder};

#[no_mangle]
pub extern "system" fn Java_com_example_domain_services_pdf_TectonicBridge_compileToPdf(
    mut env: JNIEnv,
    _class: JClass,
    tex_source: JString,
    bundle_path: JString,
    output_dir: JString,
) -> jstring {
    let tex_source: String = env.get_string(&tex_source).unwrap().into();
    let bundle_path: String = env.get_string(&bundle_path).unwrap().into();
    let output_dir: String = env.get_string(&output_dir).unwrap().into();

    let mut status = tectonic::status::NoopStatusBackend::default();

        // THE FIX: Blindfold Fontconfig to prevent C++ panic on Android
    std::env::set_var("FONTCONFIG_FILE", "/dev/null");
    std::env::set_var("FONTCONFIG_PATH", "/dev/null");
    
    let mut builder = ProcessingSessionBuilder::default();
    builder
        .bundle(Box::new(tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path))))
        .primary_input_buffer(tex_source.as_bytes())
        .tex_input_name("main.tex")
        .build_date(std::time::SystemTime::now())
        .output_dir(PathBuf::from(&output_dir))
        .output_format(tectonic::driver::OutputFormat::Pdf)
        ;
    let mut session = builder.create(&mut status)
        .unwrap();

    let result = session.run(&mut status);

    let output_path = if result.is_ok() {
        let pdf_path = PathBuf::from(&output_dir).join("main.pdf");
        pdf_path.to_string_lossy().into_owned()
    } else {
        String::from("Error")
    };

    let output = env.new_string(output_path).unwrap();
    output.into_raw()
}
