use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::path::PathBuf;
use tectonic::driver::ProcessingSessionBuilder;

#[no_mangle]
pub extern "system" fn Java_com_example_domain_services_pdf_TectonicBridge_compileToPdf(
    mut env: JNIEnv,
    _class: JClass,
    tex_source: JString,
    bundle_path: JString,
    output_dir: JString,
) -> jstring {
    let tex_source: String = env.get_string(&tex_source).unwrap_or_default().into();
    let bundle_path: String = env.get_string(&bundle_path).unwrap_or_default().into();
    let output_dir: String = env.get_string(&output_dir).unwrap_or_default().into();

    let mut status = tectonic::status::NoopStatusBackend::default();

    // Blindfold Fontconfig to prevent C++ panic on Android
    std::env::set_var("FONTCONFIG_FILE", "/dev/null");
    std::env::set_var("FONTCONFIG_PATH", "/dev/null");
    std::env::set_var("TECTONIC_CACHE_DIR", &output_dir);

    let mut builder = ProcessingSessionBuilder::default();
    builder
        .bundle(Box::new(tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path))))
        .primary_input_buffer(tex_source.as_bytes())
        .tex_input_name("main.tex")
        .build_date(std::time::SystemTime::now())
        .output_dir(PathBuf::from(&output_dir))
        .output_format(tectonic::driver::OutputFormat::Pdf);

    // 🔴 THE FIX: Catch the builder error instead of unwrap() panic
    let mut session = match builder.create(&mut status) {
        Ok(s) => s,
        Err(e) => {
            let err_msg = format!("Error: Failed to create session - {}", e);
            return env.new_string(err_msg).unwrap().into_raw();
        }
    };

    // 🔴 THE FIX: Catch the execution error and return the exact reason
    let output_path = match session.run(&mut status) {
        Ok(_) => {
            let pdf_path = PathBuf::from(&output_dir).join("main.pdf");
            pdf_path.to_string_lossy().into_owned()
        }
        Err(e) => {
            format!("Error: Compilation failed - {}", e)
        }
    };

    let output = env.new_string(output_path).unwrap();
    output.into_raw()
}
