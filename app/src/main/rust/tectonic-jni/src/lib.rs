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
    let tex_source: String = env.get_string(&tex_source).map(|s| s.into()).unwrap_or_default();
    let bundle_path: String = env.get_string(&bundle_path).map(|s| s.into()).unwrap_or_default();
    let output_dir: String = env.get_string(&output_dir).map(|s| s.into()).unwrap_or_default();

    let mut status = tectonic::status::NoopStatusBackend::default();

    // 🎯 THE FIX: Give Fontconfig a real, writable directory so the C++ library doesn't abort()
    let fc_dir = PathBuf::from(&output_dir).join("fontconfig");
    std::fs::create_dir_all(&fc_dir).unwrap_or_default();
    std::env::set_var("FONTCONFIG_FILE", fc_dir.join("fonts.conf").to_string_lossy().to_string());
    std::env::set_var("FONTCONFIG_PATH", fc_dir.to_string_lossy().to_string());
    std::env::set_var("TECTONIC_CACHE_DIR", &output_dir);
    // 🌐 THE SSL FIX: Point Rust to Android's hidden CA certificates
    std::env::set_var("SSL_CERT_DIR", "/system/etc/security/cacerts");

    let mut builder = ProcessingSessionBuilder::default();
    builder
        // 🌐 THE FIX: Removed offline restriction. Tectonic will now intelligently fetch from the cloud!
        // .bundle(Box::new(tectonic_bundles::dir::DirBundle::new(std::path::PathBuf::from(&bundle_path))))
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

    // 🛡️ THE ULTIMATE SHIELD: Catch execution errors AND Native Rust Panics
    let output_path = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        session.run(&mut status)
    })) {
        Ok(Ok(_)) => {
            let pdf_path = PathBuf::from(&output_dir).join("main.pdf");
            pdf_path.to_string_lossy().into_owned()
        }
        Ok(Err(e)) => format!("Error: Compilation failed - {}", e),
        Err(panic_err) => {
            let msg = if let Some(s) = panic_err.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_err.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown Native Rust Panic".to_string()
            };
            format!("Error: Native Rust Panic Caught! - {}", msg)
        }
    };

    let output = env.new_string(output_path).unwrap();
    output.into_raw()
}
