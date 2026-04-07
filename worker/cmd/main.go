package main

import (
	"archive/zip"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"time"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Println("Usage: go run main.go <zip-file>")
		return
	}
	zipPath := os.Args[1]
	buildDir := fmt.Sprintf("./builds/%d", time.Now().UnixNano())
	fmt.Println("STEP 1 → Unzipping source...")
	err := unzip(zipPath, buildDir)
	if err != nil {
		panic(err)
	}
	imageName := fmt.Sprintf("mynimbus-%d", time.Now().UnixNano())
	fmt.Println("STEP 2 → Building docker image via nixpacks...")
	err = buildImage(buildDir, imageName)
	if err != nil {
		panic(err)
	}
	fmt.Println("Image built successfully")
	fmt.Println("RESULT: " + imageName)
}

func unzip(src, dest string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer r.Close()
	for _, f := range r.File {
		path := filepath.Join(dest, f.Name)
		if f.FileInfo().IsDir() {
			os.MkdirAll(path, os.ModePerm)
			continue
		}
		os.MkdirAll(filepath.Dir(path), os.ModePerm)
		outFile, err := os.Create(path)
		if err != nil {
			return err
		}
		rc, err := f.Open()
		if err != nil {
			return err
		}
		_, err = io.Copy(outFile, rc)
		if err != nil {
			return err
		}
		outFile.Close()
		rc.Close()
	}
	return nil
}

func buildImage(dir string, image string) error {
	cmd := exec.Command(
		"nixpacks",
		"build",
		dir,
		"--name",
		image,
	)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

// func getPort() string {
// 	rand.Seed(time.Now().UnixNano())
// 	return fmt.Sprintf("%d", 8000+rand.Intn(1000))
// }
